import { reactive } from 'vue'

export type ToastType = 'info' | 'success' | 'error'

export interface ToastMessage {
  id: number
  message: string
  type: ToastType
}

const state = reactive<{ list: ToastMessage[] }>({
  list: [],
})

let seed = 0

export function useToast() {
  return state
}

export function pushToast(message: string, type: ToastType = 'info', timeout = 2600) {
  const id = seed++
  state.list.push({ id, message, type })
  window.setTimeout(() => {
    const index = state.list.findIndex((item) => item.id === id)
    if (index >= 0) {
      state.list.splice(index, 1)
    }
  }, timeout)
}
