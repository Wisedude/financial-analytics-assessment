export const zar = (value: number) => new Intl.NumberFormat('en-ZA', { style:'currency', currency:'ZAR', maximumFractionDigits:2 }).format(value)
export const date = (value: string) => new Intl.DateTimeFormat('en-ZA', { day:'2-digit', month:'short' }).format(new Date(value))
