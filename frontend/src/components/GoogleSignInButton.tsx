import { useEffect, useRef, useState } from 'react'

interface GoogleCredentialResponse { credential: string }
interface GoogleIdentityApi {
  initialize: (options: {
    client_id: string
    callback: (response: GoogleCredentialResponse) => void
    login_hint?: string
  }) => void
  renderButton: (element: HTMLElement, options: {
    theme: 'outline'
    size: 'large'
    text: 'continue_with'
    shape: 'rectangular'
    width: number
    logo_alignment: 'left'
  }) => void
}

declare global {
  interface Window {
    google?: { accounts: { id: GoogleIdentityApi } }
  }
}

interface Props {
  clientId: string
  loginHint?: string
  disabled?: boolean
  onCredential: (credential: string) => void
  onError: (message: string) => void
}

export default function GoogleSignInButton({
  clientId, loginHint, disabled = false, onCredential, onError
}: Props) {
  const container = useRef<HTMLDivElement>(null)
  const credentialHandler = useRef(onCredential)
  const errorHandler = useRef(onError)
  const [sdkReady, setSdkReady] = useState(() => Boolean(window.google))

  useEffect(() => { credentialHandler.current = onCredential }, [onCredential])
  useEffect(() => { errorHandler.current = onError }, [onError])

  useEffect(() => {
    if (window.google) {
      setSdkReady(true)
      return
    }
    let script = document.getElementById('google-identity-services') as HTMLScriptElement | null
    if (!script) {
      script = document.createElement('script')
      script.id = 'google-identity-services'
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      document.head.appendChild(script)
    }
    const onLoad = () => setSdkReady(true)
    const onScriptError = () => errorHandler.current('Google sign-in could not be loaded. Check your connection and try again.')
    script.addEventListener('load', onLoad)
    script.addEventListener('error', onScriptError)
    return () => {
      script?.removeEventListener('load', onLoad)
      script?.removeEventListener('error', onScriptError)
    }
  }, [])

  useEffect(() => {
    if (!sdkReady || !window.google || !container.current) return
    container.current.replaceChildren()
    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: response => credentialHandler.current(response.credential),
      ...(loginHint?.trim() ? { login_hint: loginHint.trim() } : {})
    })
    window.google.accounts.id.renderButton(container.current, {
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      shape: 'rectangular',
      width: 320,
      logo_alignment: 'left'
    })
    return () => { container.current?.replaceChildren() }
  }, [clientId, loginHint, sdkReady])

  return <div className={`google-signin ${disabled ? 'is-disabled' : ''}`}>
    <div ref={container} aria-label="Continue with Google"/>
    {!sdkReady && <span className="google-signin-loading">Loading Google sign-in…</span>}
    {disabled && <span className="google-signin-overlay" aria-hidden="true"/>}
  </div>
}
