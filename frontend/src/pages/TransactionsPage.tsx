import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ArrowDown, ArrowUp, Download, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import type { CreateTransactionRequest, Transaction } from '../types'
import { date, zar } from '../utils/format'
import PageHeader from '../components/PageHeader'
import { api } from '../services/api'

interface Props {
  transactions: Transaction[]
  onTransactionChanged: (transaction?: Transaction) => Promise<void>
}

function localDateValue() {
  const today = new Date()
  const offset = today.getTimezoneOffset()
  return new Date(today.getTime() - offset * 60_000).toISOString().slice(0, 10)
}

export default function TransactionsPage({transactions,onTransactionChanged}:Props) {
  const [items,setItems]=useState(transactions)
  const [query,setQuery]=useState('')
  const [category,setCategory]=useState('All categories')
  const [type,setType]=useState('All types')
  const [sort,setSort]=useState('newest')
  const [formOpen,setFormOpen]=useState(false)
  const [editingId,setEditingId]=useState<string|null>(null)
  const [saving,setSaving]=useState(false)
  const [saveMessage,setSaveMessage]=useState('')
  const [saveError,setSaveError]=useState('')
  const [transactionDate,setTransactionDate]=useState(localDateValue)
  const [merchant,setMerchant]=useState('')
  const [transactionCategory,setTransactionCategory]=useState('')
  const [amount,setAmount]=useState('')
  const [transactionType,setTransactionType]=useState<Transaction['type']>('DEBIT')
  const categories=useMemo(()=>Array.from(new Set(items.map(transaction=>transaction.category))).sort(),[items])
  const filtered=useMemo(()=>items.filter(transaction=>{
    const matchesSearch=`${transaction.merchant} ${transaction.category} ${transaction.id}`.toLowerCase().includes(query.trim().toLowerCase())
    return matchesSearch&&(category==='All categories'||transaction.category===category)&&(type==='All types'||transaction.type===type)
  }).sort((a,b)=>sort==='oldest'?a.date.localeCompare(b.date):b.date.localeCompare(a.date)),[items,query,category,type,sort])

  useEffect(()=>setItems(transactions),[transactions])

  async function submitTransaction(event:FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    setSaveError('')
    setSaveMessage('')
    const request:CreateTransactionRequest={
      date:transactionDate,
      merchant:merchant.trim(),
      category:transactionCategory.trim(),
      amount:Number(amount),
      type:transactionType
    }
    try {
      const wasEditing=editingId!==null
      const saved=editingId
        ? await api.updateTransaction(editingId,request)
        : await api.createTransaction(request)
      setItems(current=>editingId
        ? current.map(item=>item.id===saved.id?saved:item)
        : [saved,...current])
      setSaveMessage(editingId?'Transaction updated.':'Transaction added.')
      const persistedTransaction=saved
      setEditingId(null)
      setMerchant('')
      setTransactionCategory('')
      setAmount('')
      setTransactionDate(localDateValue())
      if (wasEditing) setFormOpen(false)
      try {
        await onTransactionChanged(persistedTransaction)
      } catch {
        setSaveMessage('Transaction saved, but the dashboard could not refresh. Reload the page to see updated totals.')
      }
    } catch(error) {
      setSaveError(error instanceof Error?error.message:'Unable to add transaction.')
    } finally {
      setSaving(false)
    }
  }

  function startAddingTransaction() {
    setEditingId(null)
    setMerchant('')
    setTransactionCategory('')
    setAmount('')
    setTransactionType('DEBIT')
    setTransactionDate(localDateValue())
    setFormOpen(true)
    setSaveError('')
    setSaveMessage('')
  }

  function startEditingTransaction(transaction:Transaction) {
    setEditingId(transaction.id)
    setTransactionDate(transaction.date)
    setMerchant(transaction.merchant)
    setTransactionCategory(transaction.category)
    setAmount(Math.abs(transaction.amount).toFixed(2))
    setTransactionType(transaction.type)
    setFormOpen(true)
    setSaveError('')
    setSaveMessage('')
  }

  async function deleteTransaction(transaction:Transaction) {
    if (!window.confirm(`Delete the transaction "${transaction.merchant}"? This cannot be undone.`)) return
    setSaving(true)
    setSaveError('')
    setSaveMessage('')
    try {
      await api.deleteTransaction(transaction.id)
      setItems(current=>current.filter(item=>item.id!==transaction.id))
      setSaveMessage('Transaction deleted.')
      if (editingId===transaction.id) {
        setEditingId(null)
        setFormOpen(false)
      }
      try {
        await onTransactionChanged()
      } catch {
        setSaveMessage('Transaction deleted, but the dashboard could not refresh. Reload the page to see updated totals.')
      }
    } catch(error) {
      setSaveError(error instanceof Error?error.message:'Unable to delete transaction.')
    } finally {
      setSaving(false)
    }
  }

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
    <PageHeader title="Transactions" description="Search and review activity across your account." action={<><button className="outline-btn" type="button" aria-expanded={formOpen} aria-controls="add-transaction-form" onClick={startAddingTransaction}><Plus size={16}/> Add transaction</button><button className="outline-btn" type="button" onClick={downloadCsv}><Download size={16}/> Export CSV</button></>}/>
    {formOpen&&<section className="panel add-transaction-panel" aria-labelledby="add-transaction-title">
      <div className="panel-head"><div><h2 id="add-transaction-title">{editingId?'Edit transaction':'Add a transaction'}</h2><p>Enter a positive amount; the transaction type determines whether it is income or an expense.</p></div></div>
      <form id="add-transaction-form" className="add-transaction-form" onSubmit={submitTransaction}>
        <label>Date<input type="date" value={transactionDate} max={localDateValue()} required onChange={event=>setTransactionDate(event.target.value)}/></label>
        <label>Merchant<input type="text" value={merchant} maxLength={120} required placeholder="e.g. Grocery Market" onChange={event=>setMerchant(event.target.value)}/></label>
        <label>Category<input type="text" value={transactionCategory} maxLength={60} required list="transaction-category-options" placeholder="e.g. Groceries" onChange={event=>setTransactionCategory(event.target.value)}/><datalist id="transaction-category-options">{categories.map(item=><option key={item} value={item}/>)}</datalist></label>
        <label>Amount (ZAR)<input type="number" value={amount} min="0.01" max="999999999999.99" step="0.01" required placeholder="0.00" onChange={event=>setAmount(event.target.value)}/></label>
        <label>Type<select value={transactionType} onChange={event=>setTransactionType(event.target.value==='CREDIT'?'CREDIT':'DEBIT')}><option value="DEBIT">Expense</option><option value="CREDIT">Income</option></select></label>
        <div className="add-transaction-actions"><button className="primary-btn" type="submit" disabled={saving}>{saving?'Saving…':editingId?'Save changes':'Save transaction'}</button><button className="outline-btn" type="button" disabled={saving} onClick={()=>{setFormOpen(false);setEditingId(null)}}>Cancel</button></div>
      </form>
      {saveError&&<p className="transaction-form-message error" role="alert">{saveError}</p>}
      {saveMessage&&<p className="transaction-form-message" role="status">{saveMessage}</p>}
    </section>}
    {!formOpen&&(saveError||saveMessage)&&<p className={`transaction-form-message transaction-page-message ${saveError?'error':''}`} role={saveError?'alert':'status'}>{saveError||saveMessage}</p>}
    <section className="panel transactions-panel">
      <div className="transaction-summary"><div><strong>{filtered.length}</strong><span>{filtered.length===1?'transaction':'transactions'} shown</span></div><span>Amounts in South African rand (ZAR)</span></div>
      <div className="transaction-filters">
        <label className="search-box"><Search size={17}/><input aria-label="Search transactions" value={query} onChange={event=>setQuery(event.target.value)} placeholder="Search merchant, category or reference"/></label>
        <select aria-label="Filter by category" value={category} onChange={event=>setCategory(event.target.value)}><option>All categories</option>{categories.map(item=><option key={item}>{item}</option>)}</select>
        <select aria-label="Filter by transaction type" value={type} onChange={event=>setType(event.target.value)}><option>All types</option><option value="CREDIT">Income</option><option value="DEBIT">Expenses</option></select>
        <select aria-label="Sort by date" value={sort} onChange={event=>setSort(event.target.value)}><option value="newest">Newest first</option><option value="oldest">Oldest first</option></select>
      </div>
      <div className="table-wrap"><table className="full-transactions"><thead><tr><th>Transaction</th><th>Category</th><th>Date</th><th>Type</th><th className="amount-cell">Amount</th><th><span className="visually-hidden">Actions</span></th></tr></thead><tbody>
        {filtered.map(transaction=><tr key={transaction.id}><td><div className="merchant"><span className={`merchant-icon ${transaction.type==='CREDIT'?'credit':''}`}>{transaction.type==='CREDIT'?<ArrowDown size={15}/>:<ArrowUp size={15}/>}</span><div><strong>{transaction.merchant}</strong><small>{transaction.id}</small></div></div></td><td><span className="category-pill">{transaction.category}</span></td><td className="tx-date">{date(transaction.date)}</td><td><span className={`type-pill ${transaction.type==='CREDIT'?'type-credit':'type-debit'}`}>{transaction.type==='CREDIT'?'Income':'Expense'}</span></td><td className={`amount-cell ${transaction.amount>0?'credit-text':'debit-text'}`}>{transaction.amount>0?'+':''}{zar(transaction.amount)}</td><td className="transaction-row-actions"><button type="button" className="transaction-action-btn" aria-label={`Edit ${transaction.merchant}`} disabled={saving} onClick={()=>startEditingTransaction(transaction)}><Pencil size={14}/></button><button type="button" className="transaction-action-btn delete" aria-label={`Delete ${transaction.merchant}`} disabled={saving} onClick={()=>deleteTransaction(transaction)}><Trash2 size={14}/></button></td></tr>)}
        {filtered.length===0&&<tr><td colSpan={6} className="empty-state">No transactions match your search and filters.</td></tr>}
      </tbody></table></div>
    </section>
  </main>
}
