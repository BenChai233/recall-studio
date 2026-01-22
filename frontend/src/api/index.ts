import { request } from './http'
import type {
  Deck,
  Item,
  SessionResponse,
  SessionSummary,
  Settings,
  TodayStats,
  ReviewResult,
  UndoReviewResult,
  ReasonDistribution,
  SyncStatus,
} from './types'

export function getTodayStats() {
  return request<TodayStats>('/reviews/today')
}

export function getReasonDistribution(days?: number) {
  return request<ReasonDistribution>('/insights/reasons', {
    query: days ? { days } : undefined,
  })
}

export function getSyncStatus() {
  return request<SyncStatus>('/sync/status')
}

export function markSyncSnapshot() {
  return request<SyncStatus>('/sync/snapshot', { method: 'POST' })
}

export function listDecks(params?: { keyword?: string; archived?: boolean }) {
  return request<Deck[]>('/decks', { query: params })
}

export function getDeck(deckId: string) {
  return request<Deck>(`/decks/${deckId}`)
}

export function createDeck(payload: { name: string; description?: string; tags?: string[] }) {
  return request<Deck>('/decks', { method: 'POST', body: payload })
}

export function updateDeck(
  deckId: string,
  payload: { name?: string; description?: string; tags?: string[]; archived?: boolean },
) {
  return request<Deck>(`/decks/${deckId}`, { method: 'PUT', body: payload })
}

export function deleteDeck(deckId: string) {
  return request<void>(`/decks/${deckId}`, { method: 'DELETE' })
}

export function listItems(params?: {
  deckId?: string
  type?: string
  tag?: string
  archived?: boolean
  dueBefore?: string
}) {
  return request<Item[]>('/items', { query: params })
}

export function getItem(itemId: string) {
  return request<Item>(`/items/${itemId}`)
}

export function createItem(payload: {
  deckId: string
  type: string
  prompt: string
  hint?: string
  answerMarkdown?: string
  tags?: string[]
  difficulty?: string
}) {
  return request<Item>('/items', { method: 'POST', body: payload })
}

export function updateItem(
  itemId: string,
  payload: {
    deckId?: string
    type?: string
    prompt?: string
    hint?: string
    answerMarkdown?: string
    tags?: string[]
    difficulty?: string
    archived?: boolean
  },
) {
  return request<Item>(`/items/${itemId}`, { method: 'PUT', body: payload })
}

export function archiveItem(itemId: string, archived: boolean) {
  return request<Item>(`/items/${itemId}/archive`, { method: 'PATCH', body: { archived } })
}

export function deleteItem(itemId: string) {
  return request<void>(`/items/${itemId}`, { method: 'DELETE' })
}

export function createSession(payload: {
  deckId?: string | null
  onlyWrong?: boolean
  limit?: number
}) {
  return request<SessionResponse>('/reviews/sessions', {
    method: 'POST',
    body: payload,
  })
}

export function submitReview(payload: {
  sessionId: string
  itemId: string
  score: number
  answer?: string
  reasonTags?: string[]
}) {
  return request<ReviewResult>('/reviews', {
    method: 'POST',
    body: payload,
  })
}

export function undoLastReview(sessionId: string) {
  return request<UndoReviewResult>(`/reviews/sessions/${sessionId}/undo`, {
    method: 'POST',
  })
}

export function undoItemReview(itemId: string) {
  return request<UndoReviewResult>(`/reviews/items/${itemId}/undo`, {
    method: 'POST',
  })
}

export function getSessionSummary(sessionId: string) {
  return request<SessionSummary>(`/reviews/sessions/${sessionId}/summary`)
}

export function getSettings() {
  return request<Settings>('/settings')
}

export function updateSettings(payload: Settings) {
  return request<Settings>('/settings', { method: 'PUT', body: payload })
}

export function importData(payload: { decks?: Deck[]; items?: Item[] }) {
  return request<{ importedDecks: number; importedItems: number; skippedItems: number }>(
    '/import',
    { method: 'POST', body: payload },
  )
}

export function exportData(includeReviews = false) {
  return request<{
    decks: Deck[]
    items: Item[]
    reviews: unknown[]
    settings: Settings
  }>(`/export`, { query: { includeReviews } })
}
