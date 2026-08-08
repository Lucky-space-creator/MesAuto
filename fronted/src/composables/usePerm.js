import { useUserStore } from '@/store/user'

/**
 * 权限判断 composable。
 * can('material:add')            -> 是否拥有单个权限码
 * can(['material:edit','material:del']) -> 是否拥有其中任意一个
 */
export function usePerm() {
  const userStore = useUserStore()
  const can = (code) => userStore.hasPerm(code)
  return { can }
}
