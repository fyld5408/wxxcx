<template>
  <view class="page">
    <text class="hint" v-if="loading">加载中…</text>
    <text class="error" v-else-if="error">{{ error }}</text>

    <template v-else>
      <text class="empty" v-if="!topics.length">还没有人发起投票</text>

      <view class="card" v-for="topic in topics" :key="topic.id" @click="open(topic.id)">
        <view class="head">
          <text class="title">{{ topic.title }}</text>
          <view class="vote-btn" v-if="!topic.myChoice" @click.stop="goVote(topic.id)">我要投票</view>
          <text class="voted" v-else>{{ topic.myChoice === 'SUPPORT' ? '已投支持' : '已投不支持' }}</text>
        </view>
        <view class="row">
          <text class="tag support">支持 {{ topic.supportCount }}</text>
          <text class="tag oppose">不支持 {{ topic.opposeCount }}</text>
        </view>
      </view>

      <button class="btn" @click="goCreate">发起投票</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { listTopics, type Topic } from '@/utils/api'

const topics = ref<Topic[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    topics.value = await listTopics()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function open(id: number) {
  uni.navigateTo({ url: `/pages/topic-detail/index?id=${id}` })
}

function goVote(id: number) {
  uni.navigateTo({ url: `/pages/vote-create/index?id=${id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/vote-create/index' })
}

onShow(load)
</script>

<style>
.page {
  padding: 32rpx;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2329;
}

.vote-btn {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #ffffff;
  background: #1f2329;
  border-radius: 100rpx;
  padding: 14rpx 28rpx;
}

.voted {
  flex-shrink: 0;
  font-size: 24rpx;
  color: #8f959e;
}

.row {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
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

.hint,
.empty {
  display: block;
  text-align: center;
  padding: 80rpx 0;
  font-size: 28rpx;
  color: #8f959e;
}

.error {
  display: block;
  padding: 40rpx 8rpx;
  font-size: 28rpx;
  color: #d83931;
}

.btn {
  margin-top: 40rpx;
  background: #1f2329;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
