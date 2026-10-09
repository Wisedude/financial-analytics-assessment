export interface User { id: string; name: string; email: string }
export interface AuthResponse { token: string; user: User }
export interface Summary { currency: string; income: number; spending: number; savings: number; savingsRate: number; availableBalance: number; period: string }
export interface CashFlowPoint { month: string; income: number; spending: number }
export interface CategorySpend { name: string; amount: number; budget: number; percentage: number }
export interface Transaction { id: string; date: string; merchant: string; category: string; amount: number; type: 'CREDIT'|'DEBIT' }
export interface Insight { type: 'WARNING'|'POSITIVE'|'INFO'; title: string; message: string }
export interface Dashboard { summary: Summary; cashFlow: CashFlowPoint[]; categories: CategorySpend[]; transactions: Transaction[]; insights: Insight[] }
