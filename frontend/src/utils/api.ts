type Method = 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'

const SERVICE_NAME = import.meta.env.VITE_MP_SERVICE_NAME || 'java-service'
const CLOUD_ENV = import.meta.env.VITE_MP_CLOUD_ENV || ''
const API_BASE = import.meta.env.VITE_API_BASE || ''

declare const wx: any

/**
 * 小程序走云托管 callContainer：由云托管注入 X-WX-OPENID，后端无需自己换 code。
 * H5 调试走 vite 代理，代理里同样会补一个测试用 X-WX-OPENID。
 */
export async function request<T>(path: string, method: Method = 'GET', data?: unknown): Promise<T> {
  // #ifdef MP-WEIXIN
  const res = await wx.cloud.callContainer({
    config: { env: CLOUD_ENV },
    path: `/api${path}`,
    method,
    header: {
      'X-WX-SERVICE': SERVICE_NAME,
      'content-type': 'application/json',
    },
    data: data ?? {},
  })
  if (res.statusCode < 200 || res.statusCode >= 300) {
    throw new Error(messageOf(res.data, res.statusCode))
  }
  return res.data as T
  // #endif

  // #ifdef H5
  const resp = await fetch(`${API_BASE}/api${path}`, {
    method,
    headers: { 'content-type': 'application/json' },
    body: method === 'GET' ? undefined : JSON.stringify(data ?? {}),
  })
  const text = await resp.text()
  if (!resp.ok) {
    let body: unknown = text
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      body = text
    }
    throw new Error(messageOf(body, resp.status))
  }
  return (text ? JSON.parse(text) : null) as T
  // #endif
}

/** 后端校验失败会返回 { message }，直接展示这一句比整段 JSON 有用 */
function messageOf(data: unknown, status: number) {
  if (data && typeof data === 'object' && 'message' in data) {
    return String((data as Record<string, unknown>).message)
  }
  if (typeof data === 'string' && data.trim()) {
    return data
  }
  return `HTTP ${status}`
}

export interface Me {
  id: number
  openid: string
  nickname?: string
  avatarUrl?: string
  createdAt: string
  lastSeenAt: string
}

export const getMe = () => request<Me>('/me')

export interface Address {
  id: number
  province: string
  city: string
  community: string
  building: string
  unit: string
  roomNo: string
  createdAt: string
}

export type AddressInput = Omit<Address, 'id' | 'createdAt'>

/** 未登记时后端返回 204，这里得到 null */
export const getAddress = () => request<Address | null>('/address')

export const createAddress = (input: AddressInput) => request<Address>('/address', 'POST', input)

export type Choice = 'SUPPORT' | 'OPPOSE'

export interface Topic {
  id: number
  title: string
  supportCount: number
  opposeCount: number
  /** 当前登录人在这条主题上的选择；后端 non_null，没投过时该字段不返回 */
  myChoice?: Choice | null
  createdAt: string
}

export interface VoteItem {
  address: string
  choice: Choice
  votedAt: string
}

export interface BuildingSummary {
  building: string
  unit: string
  supportCount: number
  opposeCount: number
}

export interface TopicDetail extends Topic {
  votes: VoteItem[]
  buildingSummary: BuildingSummary[]
}

export interface VoteResult {
  topicId: number
  title: string
  choice: Choice
  address: string
}

export const listTopics = () => request<Topic[]>('/topics')

export const getTopic = (id: number) => request<TopicDetail>(`/topics/${id}`)

export const submitVote = (title: string, choice: Choice) =>
  request<VoteResult>('/votes', 'POST', { title, choice })

/** 对已有主题投票，别人发起的主题走这个 */
export const voteOnTopic = (topicId: number, choice: Choice) =>
  request<VoteResult>(`/topics/${topicId}/votes`, 'POST', { choice })

