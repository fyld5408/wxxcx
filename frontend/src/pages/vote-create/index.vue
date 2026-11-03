<template>
  <view class="page">
    <view class="card">
      <text class="label">投票主题</text>
      <text v-if="lockedTitle" class="locked">{{ lockedTitle }}</text>
      <input v-else class="input" v-model="title" maxlength="64" placeholder="如 是否加装电梯" />
    </view>

    <view class="card">
      <text class="label">你的选择</text>
      <view class="choices">
        <view class="choice support" :class="{ active: choice === 'SUPPORT' }" @click="choice = 'SUPPORT'">
          支持
        </view>
        <view class="choice oppose" :class="{ active: choice === 'OPPOSE' }" @click="choice = 'OPPOSE'">
          不支持
        </view>
      </view>
    </view>

    <text class="error" v-if="error">{{ error }}</text>

    <button class="btn" :disabled="submitting" @click="submit">
      {{ submitting ? '提交中…' : '提交' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getTopic, submitVote, voteOnTopic, type Choice } from '@/utils/api'

const topicId = ref(0)
const lockedTitle = ref('')
const title = ref('')
const choice = ref<Choice | ''>('')
const submitting = ref(false)
const error = ref('')

onLoad(async (query) => {
  const id = Number(query?.id)
  if (!id) return
  topicId.value = id
  try {
    lockedTitle.value = (await getTopic(id)).title
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
})

async function submit() {
  if (!topicId.value && !title.value.trim()) {
    error.value = '请填写投票主题'
    return
  }
  const picked = choice.value
  if (!picked) {
    error.value = '请选择支持或不支持'
    return
  }

  submitting.value = true
  error.value = ''
  try {
    const result = topicId.value
      ? await voteOnTopic(topicId.value, picked)
      : await submitVote(title.value.trim(), picked)
    uni.showToast({ title: '投票成功', icon: 'success' })
    setTimeout(() => uni.redirectTo({ url: `/pages/topic-detail/index?id=${result.topicId}` }), 600)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    submitting.value = false
  }
}
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

.label {
  display: block;
  font-size: 26rpx;
  color: #8f959e;
  margin-bottom: 16rpx;
}

.input {
  font-size: 32rpx;
  color: #1f2329;
  padding: 8rpx 0;
}

.locked {
  display: block;
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2329;
  padding: 8rpx 0;
}

.choices {
  display: flex;
  gap: 24rpx;
}

.choice {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  border-radius: 16rpx;
  font-size: 30rpx;
  border: 2rpx solid #e5e6e8;
  color: #646a73;
}

.choice.support.active {
  border-color: #1a7f37;
  background: #eaf6ec;
  color: #1a7f37;
  font-weight: 600;
}

.choice.oppose.active {
  border-color: #d83931;
  background: #fdeeed;
  color: #d83931;
  font-weight: 600;
}

.error {
  display: block;
  margin: 8rpx 8rpx 0;
  font-size: 26rpx;
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
