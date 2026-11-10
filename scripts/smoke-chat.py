"""Live local smoke test: makes two paid model calls and stores synthetic test messages."""
import json
import urllib.parse
import urllib.request
import uuid

BASE = "http://127.0.0.1:3000/api"
chat_id = "smoke-" + str(uuid.uuid4())


def get(path, **params):
    url = BASE + path + "?" + urllib.parse.urlencode(params)
    return urllib.request.urlopen(url, timeout=120)


with get("/ai/love_app_chat/sync", chatId=chat_id,
         message="这是联调测试。请记住我叫小林，我想周六约朋友喝咖啡。请用一句简短中文回复。") as response:
    first = response.read().decode()
    assert response.status == 200 and first.strip(), first
    print("SYNC:", first)

with get("/ai/love_app_chat/sse_emitter", chatId=chat_id,
         message="我刚才说我叫什么、哪天想做什么？请只用一句话回答。") as response:
    assert response.headers.get("Content-Type", "").startswith("text/event-stream")
    chunks, done = [], False
    for line in response:
        line = line.decode().rstrip("\r\n")
        if line.startswith("data:"):
            value = line[5:].lstrip(" ")
            if value == "[DONE]":
                done = True
                break
            chunks.append(value)
    answer = "".join(chunks)
    assert done and answer.strip(), (done, answer)
    assert "小林" in answer and "咖啡" in answer, answer
    print("SSE:", answer)

with get("/loveApp/chatHistory", chatId=chat_id, limit=10) as response:
    body = json.load(response)
    assert body["code"] == 0, body
    messages = body["data"]
    assert [m["role"] for m in messages] == ["user", "assistant", "user", "assistant"], messages
    assert all(m["conversationId"] == chat_id for m in messages)
    print("PASS: sync, SSE [DONE], multi-turn recall, PostgreSQL history; chatId=" + chat_id)
