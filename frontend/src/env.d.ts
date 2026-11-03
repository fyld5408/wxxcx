/// <reference types="vite/client" />

// 微信小程序全局对象，仅在 MP-WEIXIN 条件编译块里使用
declare const wx: any

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>
  export default component
}
