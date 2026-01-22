import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../pages/Dashboard.vue'
import Decks from '../pages/Decks.vue'
import DeckDetail from '../pages/DeckDetail.vue'
import ItemDetail from '../pages/ItemDetail.vue'
import Review from '../pages/Review.vue'
import Insights from '../pages/Insights.vue'
import Settings from '../pages/Settings.vue'
import NotFound from '../pages/NotFound.vue'

const routes = [
  { path: '/', name: 'dashboard', component: Dashboard },
  { path: '/decks', name: 'decks', component: Decks },
  { path: '/decks/:deckId', name: 'deck-detail', component: DeckDetail, props: true },
  { path: '/decks/:deckId/items/:itemId', name: 'item-detail', component: ItemDetail },
  { path: '/review', name: 'review', component: Review },
  { path: '/insights', name: 'insights', component: Insights },
  { path: '/settings', name: 'settings', component: Settings },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFound },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
