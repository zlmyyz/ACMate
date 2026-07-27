export const platformLabels: Record<string, string> = {
  CUSTOM: '自定义',
  CODEFORCES: 'Codeforces',
  NOWCODER: '牛客',
  OTHER: '其他',
}

export const problemStatusLabels: Record<string, string> = {
  ACTIVE: '正常',
  INACTIVE: '已停用',
}

export const problemStatusFilterLabels: Record<string, string> = {
  ALL: '全部',
  ACTIVE: '正常',
  INACTIVE: '已停用',
}

export const trainingTypeLabels = {
  PERSONAL: '个人计划',
  PUBLIC: '公开计划',
} as const

export const trainingTimeStatusLabels: Record<string, string> = {
  NOT_STARTED: '未开始',
  ONGOING: '进行中',
  ENDED: '已结束',
}

export const progressLabels: Record<string, string> = {
  NOT_STARTED: '未做',
  CHALLENGING: '挑战中',
  ACCEPTED: '已通过',
}

export const syncStatusLabels: Record<string, string> = {
  PENDING: '等待中',
  RUNNING: '进行中',
  SUCCESS: '成功',
  FAILED: '失败',
}

export const postTypeLabels: Record<string, string> = {
  SOLUTION: '题解',
  QUESTION: '问题求助',
  CONTEST_SUMMARY: '竞赛总结',
  TRAINING_EXPERIENCE: '训练经验',
  ANNOUNCEMENT: '公告',
  OTHER: '其他',
}

export const navLabels = {
  home: '首页',
  problems: '题库',
  myProblems: '我的题目',
  trainingPlans: '训练计划',
  discussions: '讨论区',
  leaderboard: '排行榜',
  adminProblems: '全部题库',
  adminUsers: '用户管理',
  adminPosts: '内容管理',
  adminComments: '评论管理',
  adminSyncTasks: '同步任务',
  adminAuditLogs: '操作日志',
  adminExports: '数据导出',
}

export const actionLabels = {
  create: '创建题目',
  edit: '编辑',
  viewDetails: '查看详情',
  deactivate: '停用',
  restore: '恢复',
  search: '搜索',
  reset: '重置',
  save: '保存',
  cancel: '取消',
  confirm: '确认',
  submit: '提交',
  back: '返回',
  login: '登录',
  register: '注册',
  logout: '退出登录',
  loadMore: '加载更多',
}
