<script setup>
import { ConfigProvider } from 'ant-design-vue'
import { XProvider, zh_CN } from 'ant-design-x-vue'
const theme = { token: {
  colorPrimary: '#aa526b', colorText: '#34303a', colorTextSecondary: '#817883',
  colorBgContainer: '#ffffff', colorBorder: '#e8dfe3', borderRadius: 14,
  fontFamily: '"PingFang SC", "Microsoft YaHei", system-ui, sans-serif', fontSize: 14,
} }
</script>
<template>
  <ConfigProvider :theme="theme"><XProvider :locale="zh_CN"><RouterView /></XProvider></ConfigProvider>
</template>
