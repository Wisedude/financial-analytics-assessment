import { useEffect, useState } from 'react'
import type { AuthResponse, Dashboard as DashboardData, User } from './types'
import { api } from './services/api'
import AuthPage from './components/AuthPage'
import Sidebar from './components/Sidebar'
import Dashboard from './components/Dashboard'
import AnalyticsPage from './pages/AnalyticsPage'
import TransactionsPage from './pages/TransactionsPage'
import BudgetsPage from './pages/BudgetsPage'
import SettingsPage from './pages/SettingsPage'

export type AppTab = 'Dashboard' | 'Analytics' | 'Transactions' | 'Budgets' | 'Settings'

export default function App() {
 const [user,setUser]=useState<User|null>(()=>{try{return JSON.parse(localStorage.getItem('fintrack_user')||'null')}catch{return null}})
 const [data,setData]=useState<DashboardData|null>(null); const [loading,setLoading]=useState(!!user); const [error,setError]=useState(''); const [mobileOpen,setMobileOpen]=useState(false)
 const [activeTab,setActiveTab]=useState<AppTab>('Dashboard')
 async function load(){setLoading(true);setError('');try{setData(await api.dashboard())}catch(e){localStorage.removeItem('fintrack_token');localStorage.removeItem('fintrack_user');setUser(null);setError(e instanceof Error?e.message:'Unable to load dashboard')}finally{setLoading(false)}}
 useEffect(()=>{if(user) load()},[user])
 async function changeDashboardMonth(month:string){setData(await api.dashboard(month))}
 function authenticated(auth:AuthResponse){setUser(auth.user)}
 async function logout(){try{await api.logout()}catch{}localStorage.removeItem('fintrack_token');localStorage.removeItem('fintrack_user');setUser(null);setData(null)}
 if(!user) return <AuthPage onAuthenticated={authenticated}/>
 if(loading) return <div className="loading-screen"><div className="loader"/><strong>Loading your finances…</strong></div>
 if(error||!data) return <div className="loading-screen"><strong>{error||'No dashboard data'}</strong><button className="primary-btn retry" onClick={load}>Try again</button></div>
 function navigate(tab:AppTab){setActiveTab(tab);setMobileOpen(false)}
 return <div className="app-shell"><Sidebar userName={user.name} activeTab={activeTab} onNavigate={navigate} onLogout={logout} mobileOpen={mobileOpen} closeMobile={()=>setMobileOpen(false)}/><button className="mobile-menu" onClick={()=>setMobileOpen(true)} aria-label="Open navigation">☰</button>
  {activeTab==='Dashboard'&&<Dashboard data={data} userName={user.name} onNavigate={navigate} onMonthChange={changeDashboardMonth}/>}
  {activeTab==='Analytics'&&<AnalyticsPage data={data}/>}
  {activeTab==='Transactions'&&<TransactionsPage transactions={data.transactions}/>}
  {activeTab==='Budgets'&&<BudgetsPage categories={data.categories} selectedMonth={data.selectedMonth} budgetPeriods={data.selectedMonth==='all'?Math.max(data.cashFlow.length,1):1}/>}
  {activeTab==='Settings'&&<SettingsPage user={user}/>}
 </div>
}
