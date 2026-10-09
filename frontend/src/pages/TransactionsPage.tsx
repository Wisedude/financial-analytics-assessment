import { useMemo, useState } from 'react'
import { ArrowDown, ArrowUp, Download, Search } from 'lucide-react'
import type { Transaction } from '../types'
import { date, zar } from '../utils/format'
import PageHeader from '../components/PageHeader'

export default function TransactionsPage({transactions}:{transactions:Transaction[]}) {
  const [query,setQuery]=useState('')
  const [category,setCategory]=useState('All categories')
  const [type,setType]=useState('All types')
  const [sort,setSort]=useState('newest')
  const categories=useMemo(()=>Array.from(new Set(transactions.map(transaction=>transaction.category))).sort(),[transactions])
  const filtered=useMemo(()=>transactions.filter(transaction=>{
    const matchesSearch=`${transaction.merchant} ${transaction.category} ${transaction.id}`.toLowerCase().includes(query.trim().toLowerCase())
    return matchesSearch&&(category==='All categories'||transaction.category===category)&&(type==='All types'||transaction.type===type)
  }).sort((a,b)=>sort==='oldest'?a.date.localeCompare(b.date):b.date.localeCompare(a.date)),[transactions,query,category,type,sort])

  function downloadCsv() {
    const escape=(value:string)=>`"${value.replace(/"/g,'""')}"`
    const rows=[['Date','Merchant','Category','Type','Amount','Reference'],...filtered.map(transaction=>[transaction.date,transaction.merchant,transaction.category,transaction.type,String(transaction.amount),transaction.id])]
    const blob=new Blob([rows.map(row=>row.map(escape).join(',')).join('\r\n')],{type:'text/csv;charset=utf-8'})
    const url=URL.createObjectURL(blob)
    const link=document.createElement('a')
    link.href=url;link.download='fintrack-transactions.csv';document.body.appendChild(link);link.click();link.remove()
    window.setTimeout(()=>URL.revokeObjectURL(url),0)
  }

  return <main className="dashboard-content page-content">
    <PageHeader title="Transactions" description="Search and review activity across your account." action={<button className="outline-btn" onClick={downloadCsv}><Download size={16}/> Export CSV</button>}/>
    <section className="panel transactions-panel">
      <div className="transaction-summary"><div><strong>{filtered.length}</strong><span>{filtered.length===1?'transaction':'transactions'} shown</span></div><span>Amounts in South African rand (ZAR)</span></div>
      <div className="transaction-filters">
        <label className="search-box"><Search size={17}/><input aria-label="Search transactions" value={query} onChange={event=>setQuery(event.target.value)} placeholder="Search merchant, category or reference"/></label>
        <select aria-label="Filter by category" value={category} onChange={event=>setCategory(event.target.value)}><option>All categories</option>{categories.map(item=><option key={item}>{item}</option>)}</select>
        <select aria-label="Filter by transaction type" value={type} onChange={event=>setType(event.target.value)}><option>All types</option><option value="CREDIT">Income</option><option value="DEBIT">Expenses</option></select>
        <select aria-label="Sort by date" value={sort} onChange={event=>setSort(event.target.value)}><option value="newest">Newest first</option><option value="oldest">Oldest first</option></select>
      </div>
      <div className="table-wrap"><table className="full-transactions"><thead><tr><th>Transaction</th><th>Category</th><th>Date</th><th>Type</th><th className="amount-cell">Amount</th></tr></thead><tbody>
        {filtered.map(transaction=><tr key={transaction.id}><td><div className="merchant"><span className={`merchant-icon ${transaction.type==='CREDIT'?'credit':''}`}>{transaction.type==='CREDIT'?<ArrowDown size={15}/>:<ArrowUp size={15}/>}</span><div><strong>{transaction.merchant}</strong><small>{transaction.id}</small></div></div></td><td><span className="category-pill">{transaction.category}</span></td><td className="tx-date">{date(transaction.date)}</td><td><span className={`type-pill ${transaction.type==='CREDIT'?'type-credit':'type-debit'}`}>{transaction.type==='CREDIT'?'Income':'Expense'}</span></td><td className={`amount-cell ${transaction.amount>0?'credit-text':'debit-text'}`}>{transaction.amount>0?'+':''}{zar(transaction.amount)}</td></tr>)}
        {filtered.length===0&&<tr><td colSpan={5} className="empty-state">No transactions match your search and filters.</td></tr>}
      </tbody></table></div>
    </section>
  </main>
}
