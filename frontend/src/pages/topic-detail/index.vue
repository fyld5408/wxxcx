<template>
  <view class="page">
    <text class="hint" v-if="loading">加载中…</text>
    <text class="error" v-else-if="error">{{ error }}</text>

    <template v-else-if="detail">
      <view class="card">
        <text class="title">{{ detail.title }}</text>
        <view class="row">
          <text class="tag support">支持 {{ detail.supportCount }}</text>
          <text class="tag oppose">不支持 {{ detail.opposeCount }}</text>
        </view>
        <text class="mine" v-if="detail.myChoice">
          你投了{{ detail.myChoice === 'SUPPORT' ? '支持' : '不支持' }}
        </text>
      </view>

      <view class="card">
        <text class="subtitle">投票明细（{{ detail.votes.length }}）</text>
        <text class="empty" v-if="!detail.votes.length">还没有人投票</text>
        <view class="item" v-for="(vote, index) in detail.votes" :key="index">
          <view class="item-main">
            <text class="value">{{ vote.address }}</text>
            <text class="time">{{ formatTime(vote.votedAt) }}</text>
          </view>
          <text class="tag" :class="vote.choice === 'SUPPORT' ? 'support' : 'oppose'">
            {{ vote.choice === 'SUPPORT' ? '支持' : '不支持' }}
          </text>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getTopic, type TopicDetail } from '@/utils/api'

const detail = ref<TopicDetail | null>(null)
const loading = ref(true)
const error = ref('')

async function load(id: number) {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getTopic(id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function formatTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

onLoad((query) => {
  const id = Number(query?.id)
  if (!id) {
    loading.value = false
    error.value = '缺少投票主题'
    return
  }
  load(id)
})
</script>

<style>
.page {
  padding: 32rpx;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.title {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
}

.subtitle {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #646a73;
  margin-bottom: 8rpx;
}

.row {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.mine {
  display: block;
  margin-top: 20rpx;
  font-size: 26rpx;
  color: #8f959e;
}

.tag {
  font-size: 24rpx;
  padding: 8rpx 20rpx;
  border-radius: 100rpx;
}

.tag.support {
  background: #eaf6ec;
  color: #1a7f37;
}

.tag.oppose {
  background: #fdeeed;
  color: #d83931;
}

.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f1f2;
}

.item:last-child {
  border-bottom: none;
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.value {
  font-size: 30rpx;
  color: #1f2329;
}

.time {
  font-size: 24rpx;
  color: #8f959e;
}

.hint,
.empty {
  display: block;
  padding: 40rpx 0;
  font-size: 28rpx;
  color: #8f959e;
}

.hint {
  text-align: center;
  padding: 80rpx 0;
}

.error {
  display: block;
  padding: 40rpx 8rpx;
  font-size: 28rpx;
  color: #d83931;
}
</style>
