<template>
  <view class="page">
    <!-- #ifdef H5 -->
    <view class="dev-bar">
      <text class="dev-tag">调试身份：{{ devOpenid }}</text>
      <text class="dev-btn" @click="switchOpenid">切换登录态</text>
    </view>
    <!-- #endif -->

    <view class="notice">
      <text class="notice-text">本系统所有投票信息都会公开，如介意请勿注册</text>
    </view>

    <view class="card" v-if="loading">
      <text class="hint">正在识别身份…</text>
    </view>

    <text class="error" v-else-if="error">{{ error }}</text>

    <template v-else>
      <view class="card" v-if="address">
        <text class="title">我的门牌</text>
        <text class="value">{{ address.province }} {{ address.city }} {{ address.community }}</text>
        <text class="line">{{ address.building }}-{{ address.unit }}-{{ address.roomNo }}</text>
      </view>

      <button class="btn" @click="goVote">发起投票</button>
      <button class="btn ghost" @click="goTopics">查看投票结果</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getAddress, type Address } from '@/utils/api'

const address = ref<Address | null>(null)
const loading = ref(true)
const error = ref('')

// #ifdef H5
const devOpenid = ref((/(?:^|;\s*)dev_openid=([^;]+)/.exec(document.cookie) || [])[1] || 'dev-openid')

/** H5 调试专用：改 dev_openid cookie，vite 代理会把它注入成 X-WX-OPENID */
function switchOpenid() {
  uni.showModal({
    title: '切换登录态',
    editable: true,
    placeholderText: '输入任意 openid',
    success: (res) => {
      if (!res.confirm) return
      const value = String(res.content || '').trim()
      if (value) {
        document.cookie = `dev_openid=${encodeURIComponent(value)}; path=/`
      } else {
        document.cookie = 'dev_openid=; path=/; max-age=0'
      }
      uni.reLaunch({ url: '/pages/index/index' })
    },
  })
}
// #endif

async function load() {
  loading.value = true
  error.value = ''
  try {
    const current = await getAddress()
    if (!current) {
      uni.reLaunch({ url: '/pages/register/index' })
      return
    }
    address.value = current
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function goVote() {
  uni.navigateTo({ url: '/pages/vote-create/index' })
}

function goTopics() {
  uni.navigateTo({ url: '/pages/topics/index' })
}

onShow(load)
</script>

<style>
.page {
  padding: 32rpx;
}

.notice {
  background: #fff7e8;
  border: 1rpx solid #ffe4ba;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
}

.dev-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.dev-tag {
  font-size: 22rpx;
  color: #8f959e;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dev-btn {
  font-size: 24rpx;
  color: #3370ff;
  padding: 8rpx 16rpx;
}

.notice-text {
  font-size: 26rpx;
  color: #b26205;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.title {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
  margin-bottom: 8rpx;
}

.value {
  font-size: 30rpx;
  color: #1f2329;
  word-break: break-all;
}

.line {
  font-size: 40rpx;
  font-weight: 600;
  color: #1f2329;
  margin-top: 8rpx;
}

.hint {
  font-size: 28rpx;
  color: #8f959e;
}

.error {
  font-size: 28rpx;
  color: #d83931;
}

.btn {
  margin-top: 24rpx;
  background: #1f2329;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 30rpx;
}

.btn.ghost {
  background: #ffffff;
  color: #1f2329;
  border: 1rpx solid #d0d3d6;
}
</style>
