import { BarChart3, CreditCard, LayoutDashboard, LogOut, Settings, Target, WalletCards, X } from 'lucide-react'
import type { AppTab } from '../App'

interface Props { userName: string; activeTab:AppTab; onNavigate:(tab:AppTab)=>void; onLogout:()=>void; mobileOpen:boolean; closeMobile:()=>void }
export default function Sidebar({userName,activeTab,onNavigate,onLogout,mobileOpen,closeMobile}:Props) {
 const navItem=(tab:AppTab,Icon:typeof LayoutDashboard)=><button type="button" className={activeTab===tab?'active':''} aria-current={activeTab===tab?'page':undefined} onClick={()=>onNavigate(tab)}><Icon size={19}/>{tab}</button>
 return <><div className={`sidebar-overlay ${mobileOpen?'show':''}`} onClick={closeMobile}/><aside className={`sidebar ${mobileOpen?'mobile-open':''}`}>
   <div className="brand"><div className="brand-mark"><WalletCards size={21}/></div> FinTrack <button className="close-sidebar" onClick={closeMobile}><X size={19}/></button></div>
   <div className="side-label">Overview</div><nav>{navItem('Dashboard',LayoutDashboard)}{navItem('Analytics',BarChart3)}{navItem('Transactions',CreditCard)}{navItem('Budgets',Target)}</nav>
   <div className="side-label settings-label">Account</div><nav>{navItem('Settings',Settings)}</nav>
   <div className="sidebar-bottom"><div className="avatar">{userName.charAt(0).toUpperCase()}</div><div className="user-meta"><strong>{userName}</strong><span>Personal account</span></div><button onClick={onLogout} title="Sign out"><LogOut size={18}/></button></div>
 </aside></>
}
