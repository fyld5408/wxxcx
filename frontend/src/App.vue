<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";

onLaunch(() => {
  // #ifdef MP-WEIXIN
  if (wx.cloud) {
    wx.cloud.init({ env: import.meta.env.VITE_MP_CLOUD_ENV || undefined, traceUser: true });
  }
  // #endif
});
</script>

<style>
page {
  background-color: #f5f6f8;
}
</style>
