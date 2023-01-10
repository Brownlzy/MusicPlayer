import { createRouter, createWebHistory } from 'vue-router'
import Player from '../views/Player.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Player
    }
  ]
})

export default router
