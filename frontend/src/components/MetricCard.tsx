import { ArrowDownRight, ArrowUpRight, PiggyBank, Wallet, TrendingDown, TrendingUp } from 'lucide-react'
interface Props { title:string; value:string; change?:string; positive?:boolean; icon:'income'|'spend'|'save'|'balance' }
export default function MetricCard({title,value,change,positive=true,icon}:Props) {
 const icons={income:TrendingUp,spend:TrendingDown,save:PiggyBank,balance:Wallet}; const Icon=icons[icon]
 return <div className="metric-card"><div className={`metric-icon ${icon}`}><Icon size={20}/></div><div className="metric-title">{title}</div><div className="metric-value">{value}</div>{change && <div className={`metric-change ${positive?'up':'down'}`}>{positive?<ArrowUpRight size={14}/>:<ArrowDownRight size={14}/>} {change}</div>}</div>
}
