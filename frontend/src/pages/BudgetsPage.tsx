import { AlertTriangle, CheckCircle2, Target } from 'lucide-react'
import type { CategorySpend } from '../types'
import { zar } from '../utils/format'
import PageHeader from '../components/PageHeader'

export default function BudgetsPage({categories}:{categories:CategorySpend[]}) {
  const totalBudget=categories.reduce((sum,category)=>sum+category.budget,0)
  const totalSpent=categories.reduce((sum,category)=>sum+category.amount,0)
  const remaining=totalBudget-totalSpent
  const overBudget=categories.filter(category=>category.amount>category.budget)
  return <main className="dashboard-content page-content">
    <PageHeader title="Budgets" description="Track category spending against your monthly budget limits."/>
    <div className="budget-overview">
      <div className="budget-total panel"><div className="budget-total-icon"><Target size={20}/></div><div><span>Monthly budget used</span><strong>{zar(totalSpent)} <small>of {zar(totalBudget)}</small></strong></div><div className="budget-overview-progress"><span style={{width:`${totalBudget?Math.min(totalSpent/totalBudget*100,100):0}%`}}/></div><small>{totalBudget?(totalSpent/totalBudget*100).toFixed(1):'0.0'}% used</small></div>
      <div className="budget-stat panel"><span>Remaining</span><strong className={remaining<0?'over-budget-text':''}>{zar(remaining)}</strong><small>{remaining<0?'Over budget overall':'Across all categories'}</small></div>
      <div className="budget-stat panel"><span>Categories over limit</span><strong className={overBudget.length?'over-budget-text':''}>{overBudget.length}</strong><small>{overBudget.length?'Needs attention':'All categories on track'}</small></div>
    </div>
    <section className="panel category-budgets"><div className="panel-head"><div><h2>Category budgets</h2><p>Spending compared with each monthly limit</p></div></div>
      <div className="budget-category-list">{categories.map(category=>{
        const used=category.budget?category.amount/category.budget*100:0
        const isOver=used>100
        const remainder=category.budget-category.amount
        return <article className="budget-category" key={category.name}><div className="budget-category-heading"><div><span className={`category-dot cat-${category.name.toLowerCase()}`}/><strong>{category.name}</strong></div><span className={`budget-status ${isOver?'status-over':'status-ok'}`}>{isOver?<AlertTriangle size={14}/>:<CheckCircle2 size={14}/>} {isOver?'Over budget':'On track'}</span></div>
          <div className="budget-category-values"><strong>{zar(category.amount)} <span>spent</span></strong><span>Budget {zar(category.budget)}</span></div>
          <div className={`category-budget-progress ${isOver?'is-over':''}`}><span style={{width:`${Math.min(used,100)}%`}}/></div>
          <div className="budget-category-footer"><span>{used.toFixed(1)}% used</span><strong className={isOver?'over-budget-text':''}>{isOver?`${zar(Math.abs(remainder))} over`: `${zar(remainder)} remaining`}</strong></div>
        </article>
      })}</div>
      {categories.length===0&&<div className="empty-state">No category budget data is available.</div>}
    </section>
    <p className="data-note">Budget amounts are supplied with your account analytics. Editing limits is not available for this account.</p>
  </main>
}
