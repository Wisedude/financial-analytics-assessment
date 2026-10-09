import type { ReactNode } from 'react'

interface Props { title:string; description:string; action?:ReactNode }

export default function PageHeader({title,description,action}:Props) {
  return <header className="topbar page-heading">
    <div><div className="breadcrumb">Personal finances <span>/</span> {title}</div><h1>{title}</h1><p>{description}</p></div>
    {action&&<div className="top-actions">{action}</div>}
  </header>
}
