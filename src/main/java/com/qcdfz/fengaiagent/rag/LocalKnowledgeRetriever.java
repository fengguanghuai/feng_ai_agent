package com.qcdfz.fengaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/** Local lexical retrieval: no embedding API, vector database or document upload. */
public final class LocalKnowledgeRetriever implements DocumentRetriever {
    private static final Pattern WORDS = Pattern.compile("[\\p{IsHan}]+|[a-z0-9]+");
    private static final String STOP_CHARS = "的了和是在我你他她它们这那一个也就都很有吗呢吧啊不与及如何什么怎么怎样是否可以为让到中时对";
    private static final Set<String> STOP_WORDS = Set.of("怎么", "怎样", "如何", "什么", "是否", "可以", "我们", "你们", "他们", "the", "and", "is", "to", "of");
    private final List<Entry> entries;
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final double averageLength;
    private final int topK;
    private final double minimumScore;
    private record Entry(Document document, Map<String, Integer> terms, int length) {}
    private record Hit(Entry entry, double score) {}

    public LocalKnowledgeRetriever(ResourcePatternResolver resolver, String location, int topK) throws IOException {
        Assert.isTrue(location.startsWith("classpath:") || location.startsWith("classpath*:") || location.startsWith("file:"),
                "Local knowledge location must use classpath: or file:");
        Assert.isTrue(topK >= 1 && topK <= 10, "Local knowledge top-k must be between 1 and 10");
        this.topK = topK;
        Resource[] resources = resolver.getResources(location);
        Assert.isTrue(resources.length > 0, "No local knowledge documents found");
        Arrays.sort(resources, Comparator.comparing(r -> Objects.requireNonNull(r.getFilename())));
        var loaded = new ArrayList<Entry>();
        for (Resource resource : resources) {
            String text = resource.getContentAsString(StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
            if (text.startsWith("\uFEFF")) text = text.substring(1);
            String title = text.lines().filter(s -> s.startsWith("# ")).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Knowledge document requires a title: " + resource.getFilename())).substring(2);
            String[] sections = text.split("(?m)^##[ \\t]+");
            Assert.isTrue(sections.length > 1, "Knowledge document requires level-two question headings: " + resource.getFilename());
            for (int i = 1; i < sections.length; i++) {
                String section = sections[i].strip();
                int newline = section.indexOf('\n');
                Assert.isTrue(newline > 0 && !section.substring(newline).isBlank(), "Empty knowledge section");
                String heading = section.substring(0, newline).strip();
                String content = "文档：" + title + "\n条目：" + section;
                var document = new Document(content, Map.of("filename", resource.getFilename(), "title", title, "heading", heading));
                var terms = terms(heading + "\n" + section); // modest heading boost
                int length = terms.values().stream().mapToInt(Integer::intValue).sum();
                loaded.add(new Entry(document, Map.copyOf(terms), length));
                terms.keySet().forEach(term -> documentFrequency.merge(term, 1, Integer::sum));
            }
        }
        entries = List.copyOf(loaded);
        // A one-question external corpus must still match a single useful word.
        minimumScore = Math.min(1.0, Math.log1p((entries.size() - 0.5) / 1.5));
        averageLength = entries.stream().mapToInt(Entry::length).average().orElseThrow();
    }

    @Override
    public List<Document> retrieve(Query query) {
        var terms = terms(query.text());
        if (terms.isEmpty()) return List.of();
        // ponytail: linear BM25 scan fits this 59-question corpus; use a local search index if it grows substantially.
        return entries.stream().map(entry -> new Hit(entry, score(entry, terms.keySet())))
                .filter(hit -> hit.score() >= minimumScore)
                .sorted(Comparator.comparingDouble(Hit::score).reversed()
                        .thenComparing(hit -> hit.entry().document().getMetadata().get("filename").toString())
                        .thenComparing(hit -> hit.entry().document().getMetadata().get("heading").toString()))
                .limit(topK).map(hit -> hit.entry().document()).toList();
    }

    private double score(Entry entry, Set<String> queryTerms) {
        double score = 0;
        for (String term : queryTerms) {
            int frequency = entry.terms().getOrDefault(term, 0);
            if (frequency == 0) continue;
            double idf = Math.log1p((entries.size() - documentFrequency.get(term) + 0.5) / (documentFrequency.get(term) + 0.5));
            double weight = term.codePointCount(0, term.length()) == 1 && Character.UnicodeScript.of(term.codePointAt(0)) == Character.UnicodeScript.HAN ? 0.2 : 1.0;
            score += weight * idf * frequency * 2.2 / (frequency + 1.2 * (0.25 + 0.75 * entry.length() / averageLength));
        }
        return score;
    }

    private static Map<String, Integer> terms(String text) {
        var terms = new HashMap<String, Integer>();
        var matcher = WORDS.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (Character.UnicodeScript.of(word.codePointAt(0)) != Character.UnicodeScript.HAN) {
                if (!STOP_WORDS.contains(word)) terms.merge(word, 1, Integer::sum);
                continue;
            }
            int[] chars = word.codePoints().toArray();
            for (int i = 0; i < chars.length; i++) {
                String single = new String(chars, i, 1);
                if (!STOP_CHARS.contains(single)) terms.merge(single, 1, Integer::sum);
                if (i + 1 < chars.length) {
                    String pair = new String(chars, i, 2);
                    if (!STOP_WORDS.contains(pair)) terms.merge(pair, 1, Integer::sum);
                }
            }
        }
        return terms;
    }

    public int documentCount() { return entries.size(); }
}
