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

      <view class="card" v-if="detail.buildingSummary.length">
        <text class="subtitle">楼栋投票汇总</text>
        <view class="summary-head">
          <text class="col-key">栋-单元</text>
          <text class="col-count">支持</text>
          <text class="col-count">不支持</text>
          <text class="col-count">合计</text>
        </view>
        <view class="summary-row" v-for="(row, index) in detail.buildingSummary" :key="index">
          <text class="col-key">{{ row.building }}-{{ row.unit }}</text>
          <text class="col-count support-num">{{ row.supportCount }}</text>
          <text class="col-count oppose-num">{{ row.opposeCount }}</text>
          <text class="col-count">{{ row.supportCount + row.opposeCount }}</text>
        </view>
      </view>

      <view class="card">
        <view class="detail-head">
          <text class="subtitle">投票明细（{{ detail.votes.length }}）</text>
          <text class="expand" v-if="keyword" @click="clearKeyword">清空搜索</text>
        </view>
        <input
          class="search"
          v-model="keyword"
          placeholder="输入 栋 / 单元 / 房号 搜索，如 7-1-902 或 902"
        />
        <text class="empty" v-if="!detail.votes.length">还没有人投票</text>
        <text class="empty" v-else-if="!filteredVotes.length">没有匹配的投票</text>
        <view class="item" v-for="(vote, index) in shownVotes" :key="index">
          <view class="item-main">
            <text class="value">{{ vote.address }}</text>
            <text class="time">{{ formatTime(vote.votedAt) }}</text>
          </view>
          <text class="tag" :class="vote.choice === 'SUPPORT' ? 'support' : 'oppose'">
            {{ vote.choice === 'SUPPORT' ? '支持' : '不支持' }}
          </text>
        </view>
        <text class="expand" v-if="matched && filteredVotes.length > shownVotes.length">
          匹配 {{ filteredVotes.length }} 条，仅显示前 {{ SEARCH_LIMIT }} 条，请缩小搜索范围
        </text>
        <text class="expand" v-else-if="!matched && shownVotes.length < filteredVotes.length" @click="visibleCount += PAGE_SIZE">
          加载更多（剩余 {{ filteredVotes.length - shownVotes.length }}）
        </text>
        <text class="expand" v-else-if="!matched && visibleCount > INITIAL_SIZE" @click="visibleCount = INITIAL_SIZE">收起</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, onUnmounted, ref, watch } from 'vue'
import { getTopic, type TopicDetail } from '@/utils/api'

/** 一次渲染的明细条数：小程序 setData 撑不住上千条节点 */
const INITIAL_SIZE = 5
const PAGE_SIZE = 50
const SEARCH_LIMIT = 50

const detail = ref<TopicDetail | null>(null)
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const debouncedKeyword = ref('')
const visibleCount = ref(INITIAL_SIZE)

const matched = computed(() => debouncedKeyword.value.trim() !== '')

let debounceTimer: ReturnType<typeof setTimeout> | undefined
watch(keyword, (value) => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    debouncedKeyword.value = value
  }, 250)
})
onUnmounted(() => clearTimeout(debounceTimer))

function clearKeyword() {
  clearTimeout(debounceTimer)
  keyword.value = ''
  debouncedKeyword.value = ''
}

const filteredVotes = computed(() => {
  const votes = detail.value?.votes ?? []
  const kw = debouncedKeyword.value.trim()
  if (!kw) return votes
  return votes.filter((v) => v.address.includes(kw))
})

const shownVotes = computed(() =>
  filteredVotes.value.slice(0, matched.value ? SEARCH_LIMIT : visibleCount.value),
)

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

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-head,
.summary-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
}

.summary-head {
  font-size: 24rpx;
  color: #8f959e;
  border-bottom: 1rpx solid #f0f1f2;
}

.summary-row {
  font-size: 28rpx;
  border-bottom: 1rpx solid #f0f1f2;
}

.summary-row:last-child {
  border-bottom: none;
}

.col-key {
  flex: 2;
  color: #1f2329;
}

.col-count {
  flex: 1;
  text-align: center;
  color: #1f2329;
}

.support-num {
  color: #1a7f37;
}

.oppose-num {
  color: #d83931;
}

.search {
  background: #f5f6f7;
  border-radius: 12rpx;
  padding: 16rpx 24rpx;
  font-size: 28rpx;
  margin: 16rpx 0 8rpx;
}

.expand {
  display: block;
  text-align: center;
  padding: 24rpx 0 4rpx;
  font-size: 26rpx;
  color: #3370ff;
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
