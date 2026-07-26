import { getCsrfToken } from './auth'

export async function withCsrf<T>(
  requestFn: (csrfHeader: string, csrfToken: string) => Promise<T>,
): Promise<T> {
  const csrf = await getCsrfToken()
  return requestFn(csrf.headerName, csrf.token)
}
