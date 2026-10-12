import { useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, CalendarDays, Download, MoreHorizontal, Search, Sparkles } from 'lucide-react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { Dashboard as DashboardData } from '../types'
import { date, zar } from '../utils/format'
import MetricCard from './MetricCard'
import type { AppTab } from '../App'

export default function Dashboard({data,userName,onNavigate,onMonthChange}:{data:DashboardData;userName:string;onNavigate:(tab:AppTab)=>void;onMonthChange:(month:string)=>Promise<void>}) {
 const [query,setQuery]=useState('')
 const [monthError,setMonthError]=useState('')
 const [changingMonth,setChangingMonth]=useState(false)
 const filtered=useMemo(()=>data.transactions.filter(t=>(t.merchant+' '+t.category).toLowerCase().includes(query.toLowerCase())),[data.transactions,query])
 const s=data.summary
 const budgetPeriods=data.selectedMonth==='all'?Math.max(data.cashFlow.length,1):1
 const totalBudget=data.categories.reduce((total,category)=>total+category.budget*budgetPeriods,0)
 const budgetRemaining=totalBudget-s.spending
 async function selectMonth(month:string){
  setMonthError('')
  setChangingMonth(true)
  try{await onMonthChange(month)}catch(error){setMonthError(error instanceof Error?error.message:'Unable to load data for this month')}finally{setChangingMonth(false)}
 }
 return <div className="dashboard-content">
   <header className="topbar"><div><div className="breadcrumb">Personal finances <span>/</span> Overview</div><h1>Good afternoon, {userName.split(' ')[0]} 👋</h1><p>Here’s what’s happening with your money.</p></div><div className="top-actions"><label className="month-picker"><CalendarDays size={17}/><span className="visually-hidden">Dashboard date range</span><select aria-label="Dashboard date range" value={data.selectedMonth} disabled={changingMonth} onChange={event=>selectMonth(event.target.value)}><option value="all">All data</option>{data.availableMonths.map(month=><option value={month.value} key={month.value}>{month.label}</option>)}</select></label><button className="icon-btn" aria-label="Download dashboard"><Download size={18}/></button></div></header>
   {monthError&&<div className="error-box month-error" role="alert">{monthError}</div>}
   <div className="metric-grid"><MetricCard title={data.selectedMonth==='all'?'Total income':'Monthly income'} value={zar(s.income)} change={s.period} positive icon="income"/><MetricCard title="Total spending" value={zar(s.spending)} change={s.period} positive={false} icon="spend"/><MetricCard title="Savings rate" value={`${s.savingsRate.toFixed(1)}%`} change={`${zar(s.savings)} saved · ${s.period}`} positive icon="save"/><MetricCard title="Available balance" value={zar(s.availableBalance)} change="Current account balance" positive icon="balance"/></div>
   <div className="content-grid">
    <section className="panel cashflow-panel"><div className="panel-head"><div><h2>Cash flow</h2><p>{data.selectedMonth==='all'?'Income and spending across all available history':`Income and spending for ${s.period}`}</p></div></div><div className="legend"><span><i className="income-dot"/> Income</span><span><i className="spend-dot"/> Spending</span></div><div className="chart"><ResponsiveContainer width="100%" height="100%"><AreaChart data={data.cashFlow} margin={{top:10,right:4,left:0,bottom:0}}><defs><linearGradient id="incomeFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopOpacity={0.22}/><stop offset="95%" stopOpacity={0}/></linearGradient><linearGradient id="spendFill" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopOpacity={0.12}/><stop offset="95%" stopOpacity={0}/></linearGradient></defs><CartesianGrid vertical={false} strokeDasharray="3 3"/><XAxis dataKey="month" axisLine={false} tickLine={false}/><YAxis axisLine={false} tickLine={false} tickFormatter={v=>`R${v/1000}k`} width={48}/><Tooltip formatter={(v:any)=>zar(v)} contentStyle={{borderRadius:12,border:'1px solid #e2e8f0'}}/><Area type="monotone" dataKey="income" strokeWidth={2.5} fill="url(#incomeFill)"/><Area type="monotone" dataKey="spending" strokeWidth={2.5} fill="url(#spendFill)"/></AreaChart></ResponsiveContainer></div></section>
    <section className="panel"><div className="panel-head"><div><h2>Spending breakdown</h2><p>Where your money went · {s.period}</p></div><button className="more-btn" aria-label="Spending breakdown options"><MoreHorizontal size={19}/></button></div><div className="category-list">{data.categories.slice(0,6).map(c=><div className="category-row" key={c.name}><div className="category-main"><span className={`category-dot cat-${c.name.toLowerCase()}`}/><div><strong>{c.name}</strong><small>{c.percentage.toFixed(1)}% of spend</small></div></div><div className="category-amount"><strong>{zar(c.amount)}</strong><div className="progress"><span style={{width:`${Math.min(c.amount/(c.budget*budgetPeriods)*100,100)}%`}}/></div></div></div>)}</div></section>
   </div>
   <div className="content-grid lower">
    <section className="panel"><div className="panel-head"><div><h2>Recent transactions</h2><p>{data.selectedMonth==='all'?'Latest account activity':`Activity in ${s.period}`}</p></div><button className="text-btn" onClick={()=>onNavigate('Transactions')}>View all <ArrowUp size={14}/></button></div><div className="search-box"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Search transactions..."/></div><div className="transactions"><div className="transaction-head"><span>Transaction</span><span>Category</span><span>Date</span><span>Amount</span></div>{filtered.slice(0,6).map(t=><div className="transaction" key={t.id}><div className="merchant"><span className={`merchant-icon ${t.type==='CREDIT'?'credit':''}`}>{t.type==='CREDIT'?<ArrowDown size={15}/>:<ArrowUp size={15}/>}</span><div><strong>{t.merchant}</strong><small>{t.id}</small></div></div><span className="category-pill">{t.category}</span><span className="tx-date">{date(t.date)}</span><strong className={t.amount>0?'credit-text':'debit-text'}>{t.amount>0?'+':''}{zar(t.amount)}</strong></div>)}</div>{filtered.length===0&&<p className="empty-transactions">No transactions match this month and search.</p>}</section>
    <section className="panel insights-panel"><div className="panel-head"><div><h2>Financial insights</h2><p>{s.period}</p></div><Sparkles size={19}/></div><div className="insight-list">{data.insights.map(i=><div className={`insight ${i.type.toLowerCase()}`} key={i.title}><div className="insight-dot"/><div><strong>{i.title}</strong><p>{i.message}</p></div></div>)}</div><div className="budget-card"><div><span>{data.selectedMonth==='all'?'Budget across selected history':'Monthly budget'}</span><strong>{zar(s.spending)} <small>/ {zar(totalBudget)}</small></strong></div><div className="budget-progress"><span style={{width:`${totalBudget?Math.min(s.spending/totalBudget*100,100):0}%`}}/></div><small>{totalBudget?(s.spending/totalBudget*100).toFixed(1):'0.0'}% used · {zar(budgetRemaining)} remaining</small></div></section>
   </div>
 </div>
}
