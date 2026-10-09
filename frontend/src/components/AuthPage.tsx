import { FormEvent, useState } from 'react'
import { ArrowRight, BarChart3, LockKeyhole, Mail, UserRound, WalletCards } from 'lucide-react'
import type { AuthResponse } from '../types'
import { api } from '../services/api'

interface Props { onAuthenticated: (auth: AuthResponse) => void }

export default function AuthPage({ onAuthenticated }: Props) {
  const [register, setRegister] = useState(false)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const auth = register ? await api.register(name, email, password) : await api.login(email, password)
      localStorage.setItem('fintrack_token', auth.token); localStorage.setItem('fintrack_user', JSON.stringify(auth.user)); onAuthenticated(auth)
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to authenticate') }
    finally { setBusy(false) }
  }

  return <main className="auth-shell">
    <section className="auth-visual">
      <div className="brand"><div className="brand-mark"><WalletCards size={21}/></div> FinTrack</div>
      <div className="visual-copy"><span className="eyebrow">FINANCIAL WELLNESS</span><h1>Understand your money. <em>Plan what’s next.</em></h1><p>One clear view of your cash flow, spending habits and financial goals.</p></div>
      <div className="visual-cards"><div><BarChart3 size={20}/><strong>35.3%</strong><span>monthly savings rate</span></div><div><WalletCards size={20}/><strong>R58,240</strong><span>available balance</span></div></div>
    </section>
    <section className="auth-panel">
      <div className="auth-card"><div className="mobile-brand"><div className="brand-mark"><WalletCards size={21}/></div> FinTrack</div><div className="auth-heading"><h2>{register ? 'Create your account' : 'Welcome back'}</h2><p>{register ? 'Start understanding your spending today.' : 'Sign in to view your financial dashboard.'}</p></div>
        <form onSubmit={submit}>
          {register && <label><span>Name</span><div className="input-wrap"><UserRound size={18}/><input value={name} onChange={e=>setName(e.target.value)} placeholder="Your name" required/></div></label>}
          <label><span>Email address</span><div className="input-wrap"><Mail size={18}/><input type="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com" required/></div></label>
          <label><span>Password</span><div className="input-wrap"><LockKeyhole size={18}/><input type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="••••••••" minLength={8} required/></div></label>
          {error && <div className="error-box">{error}</div>}
          <button className="primary-btn" disabled={busy}>{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'} <ArrowRight size={18}/></button>
        </form>
        {!register && <div className="demo-hint"><strong>New to FinTrack?</strong><span>Create an account to get a persistent sample financial history.</span></div>}
        <p className="switch-auth">{register ? 'Already have an account?' : 'New to FinTrack?'} <button onClick={()=>{setRegister(!register);setError('')}}>{register ? 'Sign in' : 'Create an account'}</button></p>
      </div>
    </section>
  </main>
}
