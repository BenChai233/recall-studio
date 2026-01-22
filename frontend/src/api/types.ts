export interface Deck {
  deckId: string
  name: string
  description?: string
  tags?: string[]
  archived?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface SrsState {
  easiness?: number
  interval?: number
  due?: string
  reps?: number
  lastScore?: number
  lastReviewedAt?: string
  wrongStep?: number | null
  wrongDue?: string | null
}

export interface Item {
  itemId: string
  deckId: string
  type: string
  prompt: string
  hint?: string
  answerMarkdown?: string
  tags?: string[]
  difficulty?: string
  archived?: boolean
  srs?: SrsState
  createdAt?: string
  updatedAt?: string
}

export interface Settings {
  dailyLimit: number
  newRatio: number
  scoreMap: Record<string, string>
  srsAlgo: string
  wrongReviewSchedule: number[]
}

export interface TodayStats {
  dueCount: number
  wrongCount: number
  newCount: number
  totalPlanned: number
}

export interface SessionItem {
  itemId: string
  prompt: string
  type: string
  dueType: 'DUE' | 'WRONG' | 'NEW'
}

export interface SessionResponse {
  sessionId: string
  items: SessionItem[]
}

export interface ReviewResult {
  nextDue: string
  srs: SrsState
}

export interface SessionSummary {
  avgScore: number
  scoreCount: Record<string, number>
  wrongItems: Array<{ itemId: string; prompt: string }>
  nextQuestions: string[]
}

export interface ReasonStat {
  tag: string
  count: number
}

export interface ReasonDistribution {
  wrongReviewCount: number
  totalTagCount: number
  stats: ReasonStat[]
}

export interface SyncStatus {
  hasSnapshot: boolean
  lastSyncAt?: string | null
  dirty: boolean
  fileCount: number
  lastModifiedAt?: string | null
}
