import { useEffect, useState } from 'react'
import { Bell, Check, LockKeyhole, Mail, ShieldCheck, UserRound } from 'lucide-react'
import type { User } from '../types'
import PageHeader from '../components/PageHeader'

interface Preferences { budgetAlerts:boolean; insightsUpdates:boolean }
const defaults:Preferences={budgetAlerts:true,insightsUpdates:true}

function readPreferences():Preferences {
  try {
    const stored=localStorage.getItem('fintrack_preferences')
    return stored?{...defaults,...JSON.parse(stored) as Partial<Preferences>}:defaults
  } catch {
    return defaults
  }
}

export default function SettingsPage({user}:{user:User}) {
  const [preferences,setPreferences]=useState(readPreferences)
  const [saveError,setSaveError]=useState('')
  useEffect(()=>{
    try { localStorage.setItem('fintrack_preferences',JSON.stringify(preferences));setSaveError('') }
    catch { setSaveError('Your preferences could not be saved on this device.') }
  },[preferences])
  function toggle(key:keyof Preferences) { setPreferences(current=>({...current,[key]:!current[key]})) }
  return <main className="dashboard-content page-content">
    <PageHeader title="Settings" description="Manage your account details and local preferences."/>
    <section className="panel settings-panel">
      <div className="settings-section-heading"><div className="settings-section-icon"><UserRound size={19}/></div><div><h2>Profile</h2><p>Your FinTrack account information</p></div></div>
      <div className="profile-details"><div className="profile-avatar">{user.name.charAt(0).toUpperCase()}</div><div className="profile-fields"><div className="profile-field"><span>Full name</span><strong>{user.name}</strong></div><div className="profile-field"><span>Email address</span><strong>{user.email}</strong></div></div></div>
      <div className="settings-divider"/>
      <div className="settings-section-heading"><div className="settings-section-icon"><Bell size={19}/></div><div><h2>Notifications</h2><p>Choose which account updates to show on this device</p></div></div>
      <div className="preference-row"><div className="preference-icon"><Mail size={17}/></div><div className="preference-copy"><strong>Financial insights</strong><span>Show updates when new spending insights are available.</span></div><button type="button" role="switch" aria-checked={preferences.insightsUpdates} aria-label="Financial insights notifications" className={`toggle-switch ${preferences.insightsUpdates?'enabled':''}`} onClick={()=>toggle('insightsUpdates')}><span/></button></div>
      <div className="preference-row"><div className="preference-icon"><Bell size={17}/></div><div className="preference-copy"><strong>Budget alerts</strong><span>Show a reminder when a category exceeds its budget.</span></div><button type="button" role="switch" aria-checked={preferences.budgetAlerts} aria-label="Budget alerts" className={`toggle-switch ${preferences.budgetAlerts?'enabled':''}`} onClick={()=>toggle('budgetAlerts')}><span/></button></div>
      {saveError?<p className="settings-save-error">{saveError}</p>:<p className="settings-saved"><Check size={14}/> Preferences saved on this device</p>}
      <div className="settings-divider"/>
      <div className="settings-section-heading"><div className="settings-section-icon"><ShieldCheck size={19}/></div><div><h2>Sign-in &amp; security</h2><p>How access to this account is protected</p></div></div>
      <div className="security-row"><div className="security-icon"><LockKeyhole size={18}/></div><div><strong>Password sign-in</strong><span>Your password is protected using a one-way hash.</span></div><span className="security-status">Enabled</span></div>
      <div className="security-row"><div className="security-icon"><ShieldCheck size={18}/></div><div><strong>Session</strong><span>You are signed in to your personal account.</span></div><span className="security-status">Active</span></div>
    </section>
    <p className="data-note">Profile and security details are managed by your account service. Notification preferences are stored locally in this browser.</p>
  </main>
}
