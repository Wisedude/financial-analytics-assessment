import { Area, AreaChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Activity, ArrowDownRight, ArrowUpRight } from 'lucide-react'
import type { Dashboard } from '../types'
import { zar } from '../utils/format'
import PageHeader from '../components/PageHeader'

const colors=['#536fe0','#55a77a','#e4a269','#8a75d6','#48a4b2','#dd7480','#9aabc4','#c4a24c']

export default function AnalyticsPage({data}:{data:Dashboard}) {
  const {summary,cashFlow,categories}=data
  const averageIncome=cashFlow.length?cashFlow.reduce((total,month)=>total+month.income,0)/cashFlow.length:0
  const averageSpending=cashFlow.length?cashFlow.reduce((total,month)=>total+month.spending,0)/cashFlow.length:0
  return <main className="dashboard-content page-content">
    <PageHeader title="Analytics" description="Explore your income, spending and savings trends."/>
    <div className="metric-grid analytics-metrics">
      <div className="metric-card"><div className="metric-icon income"><Activity size={20}/></div><div className="metric-title">Average monthly income</div><div className="metric-value">{zar(averageIncome)}</div><div className="metric-change up"><ArrowUpRight size={14}/> {cashFlow.length} months tracked</div></div>
      <div className="metric-card"><div className="metric-icon spend"><Activity size={20}/></div><div className="metric-title">Average monthly spending</div><div className="metric-value">{zar(averageSpending)}</div><div className="metric-change down"><ArrowDownRight size={14}/> {summary.period}</div></div>
      <div className="metric-card"><div className="metric-icon save"><Activity size={20}/></div><div className="metric-title">Savings this period</div><div className="metric-value">{zar(summary.savings)}</div><div className="metric-change up">{summary.savingsRate.toFixed(1)}% savings rate</div></div>
      <div className="metric-card"><div className="metric-icon balance"><Activity size={20}/></div><div className="metric-title">Available balance</div><div className="metric-value">{zar(summary.availableBalance)}</div><div className="metric-change up">Current balance</div></div>
    </div>
    <div className="content-grid analytics-grid">
      <section className="panel"><div className="panel-head"><div><h2>Income vs. spending</h2><p>Monthly cash flow for the last {cashFlow.length} months</p></div></div>
        <div className="legend"><span><i className="income-dot"/> Income</span><span><i className="spend-dot"/> Spending</span></div>
        <div className="chart analytics-chart"><ResponsiveContainer width="100%" height="100%"><AreaChart data={cashFlow} margin={{top:10,right:8,left:0,bottom:0}}><defs><linearGradient id="analyticsIncome" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#536fe0" stopOpacity={0.2}/><stop offset="95%" stopColor="#536fe0" stopOpacity={0}/></linearGradient><linearGradient id="analyticsSpending" x1="0" y1="0" x2="0" y2="1"><stop offset="5%" stopColor="#e4a269" stopOpacity={0.17}/><stop offset="95%" stopColor="#e4a269" stopOpacity={0}/></linearGradient></defs><CartesianGrid vertical={false} strokeDasharray="3 3"/><XAxis dataKey="month" axisLine={false} tickLine={false}/><YAxis axisLine={false} tickLine={false} tickFormatter={value=>`R${Math.round(value/1000)}k`} width={48}/><Tooltip formatter={value=>zar(Number(value))} contentStyle={{borderRadius:12,border:'1px solid #e2e8f0'}}/><Area type="monotone" dataKey="income" name="Income" stroke="#536fe0" strokeWidth={2.5} fill="url(#analyticsIncome)"/><Area type="monotone" dataKey="spending" name="Spending" stroke="#e4a269" strokeWidth={2.5} fill="url(#analyticsSpending)"/></AreaChart></ResponsiveContainer></div>
      </section>
      <section className="panel"><div className="panel-head"><div><h2>Spending by category</h2><p>Category share for {summary.period}</p></div></div>
        <div className="category-chart"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={categories} dataKey="amount" nameKey="name" innerRadius="58%" outerRadius="82%" paddingAngle={2}>{categories.map((category,index)=><Cell key={category.name} fill={colors[index%colors.length]}/>)}</Pie><Tooltip formatter={value=>zar(Number(value))}/></PieChart></ResponsiveContainer><div className="category-chart-center"><strong>{zar(summary.spending)}</strong><span>Total spending</span></div></div>
        <div className="analytics-category-list">{categories.map((category,index)=><div className="analytics-category" key={category.name}><i style={{backgroundColor:colors[index%colors.length]}}/><span>{category.name}</span><strong>{category.percentage.toFixed(1)}%</strong></div>)}</div>
      </section>
    </div>
  </main>
}
