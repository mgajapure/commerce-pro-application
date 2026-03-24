import { Routes } from '@angular/router';

export const AI_ROUTES: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/ai-dashboard').then(m => m.AiDashboard)
  },
  {
    path: 'config',
    loadComponent: () => import('./config/ai-config').then(m => m.AiConfig)
  },
  {
    path: 'nl-report',
    loadComponent: () => import('./nl-report/ai-nl-report').then(m => m.AiNlReport)
  },
  {
    path: 'chatbot-sessions',
    loadComponent: () => import('./chatbot-sessions/ai-chatbot-sessions').then(m => m.AiChatbotSessions)
  },
  {
    path: 'insights',
    loadComponent: () => import('./insights/ai-insights').then(m => m.AiInsights)
  }
];
