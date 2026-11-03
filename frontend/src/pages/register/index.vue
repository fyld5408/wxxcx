<template>
  <view class="page">
    <view class="notice">
      <text class="notice-text">本系统所有投票信息都会公开，如介意请勿注册</text>
    </view>

    <view class="card">
      <view class="field">
        <text class="label">省</text>
        <picker class="picker" mode="selector" :range="PROVINCES" @change="onProvinceChange">
          <view class="picker-value" :class="{ placeholder: !form.province }">
            {{ form.province || '请选择省' }}
          </view>
        </picker>
      </view>
      <view class="field">
        <text class="label">市</text>
        <picker class="picker" mode="selector" :range="cities" :disabled="!form.province" @change="onCityChange">
          <view class="picker-value" :class="{ placeholder: !form.city }">
            {{ form.province ? (form.city || '请选择市') : '请先选择省' }}
          </view>
        </picker>
      </view>
      <view class="field">
        <text class="label">小区名称</text>
        <input class="input" v-model="form.community" placeholder="如 阳光花园" />
      </view>
      <view class="field">
        <text class="label">栋</text>
        <input class="input" v-model="form.building" placeholder="如 7" />
      </view>
      <view class="field">
        <text class="label">单元</text>
        <input class="input" v-model="form.unit" placeholder="如 1" />
      </view>
      <view class="field">
        <text class="label">房号</text>
        <input class="input" v-model="form.roomNo" placeholder="如 902" />
      </view>
    </view>

    <text class="tip">门牌会拼成 7-1-902，用于在投票结果里标识你的房屋</text>

    <text class="error" v-if="error">{{ error }}</text>

    <button class="btn" :disabled="submitting" @click="submit">
      {{ submitting ? '提交中…' : '提交' }}
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { createAddress, type AddressInput } from '@/utils/api'
import { PROVINCES, citiesOf } from '@/utils/region'

const form = reactive<AddressInput>({
  province: '',
  city: '',
  community: '',
  building: '',
  unit: '',
  roomNo: '',
})

const cities = computed(() => citiesOf(form.province))

function onProvinceChange(e: { detail: { value: number } }) {
  const province = PROVINCES[Number(e.detail.value)]
  if (province === form.province) return
  form.province = province
  const options = citiesOf(province)
  form.city = options.length === 1 ? options[0] : ''
}

function onCityChange(e: { detail: { value: number } }) {
  form.city = cities.value[Number(e.detail.value)] ?? ''
}

const submitting = ref(false)
const error = ref('')

const labels: Record<keyof AddressInput, string> = {
  province: '省',
  city: '市',
  community: '小区名称',
  building: '栋',
  unit: '单元',
  roomNo: '房号',
}

async function submit() {
  for (const key of Object.keys(labels) as (keyof AddressInput)[]) {
    if (!form[key].trim()) {
      error.value = `请填写${labels[key]}`
      return
    }
  }

  submitting.value = true
  error.value = ''
  try {
    await createAddress({
      province: form.province.trim(),
      city: form.city.trim(),
      community: form.community.trim(),
      building: form.building.trim(),
      unit: form.unit.trim(),
      roomNo: form.roomNo.trim(),
    })
    uni.showToast({ title: '提交成功', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/index/index' }), 600)
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

.notice {
  background: #fff7e8;
  border: 1rpx solid #ffe4ba;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 24rpx;
}

.notice-text {
  font-size: 26rpx;
  color: #b26205;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 8rpx 32rpx;
}

.field {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f0f1f2;
}

.field:last-child {
  border-bottom: none;
}

.label {
  width: 160rpx;
  font-size: 30rpx;
  color: #1f2329;
}

.input {
  flex: 1;
  font-size: 30rpx;
  color: #1f2329;
}

.picker {
  flex: 1;
}

.picker-value {
  font-size: 30rpx;
  color: #1f2329;
}

.picker-value.placeholder {
  color: #a9aeb2;
}

.error {
  display: block;
  margin: 24rpx 8rpx 0;
  font-size: 26rpx;
  color: #d83931;
}

.tip {
  display: block;
  margin: 24rpx 8rpx 0;
  font-size: 24rpx;
  color: #8f959e;
}

.btn {
  margin-top: 48rpx;
  background: #1f2329;
  color: #ffffff;
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
