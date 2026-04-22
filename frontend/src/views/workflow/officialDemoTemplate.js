function cloneConfig(value) {
  return JSON.parse(JSON.stringify(value))
}

function defaultCreateId(prefix = 'field') {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
}

export const DEFAULT_OFFICIAL_TEMPLATE_KEY = 'website_email_notify'

function createField(createId, name, title, description, options = {}) {
  return {
    id: createId('field'),
    name,
    title,
    type: options.type || 'string',
    description,
    required: options.required !== false,
    format: options.format || '',
    enumText: options.enumText || '',
    defaultValue: options.defaultValue || ''
  }
}

function createEmailFields(createId, extraFields = []) {
  return [
    createField(createId, 'target_url', '目标网址', '输入要抓取的网站地址', { format: 'url' }),
    ...extraFields,
    createField(createId, 'to_email', '收件邮箱', 'AI 整理后的详细内容会发送到这个邮箱地址', {
      format: 'email'
    })
  ]
}

function createWebhookFields(createId) {
  return [
    createField(createId, 'target_url', '目标网址', '输入要抓取的网站地址', { format: 'url' }),
    createField(createId, 'webhook_url', 'Webhook 地址', '抓取完成后要同步到的接口地址', { format: 'url' }),
    createField(createId, 'webhook_token', '接口令牌', '可选，用于给目标接口传递校验令牌', {
      required: false
    })
  ]
}

function detailedEmailPrompt(titleExpression, summaryExpression, urlExpression, extraInstructions = '') {
  return [
    '请基于以下网页内容，生成一封适合发送给业务同学的详细邮件。',
    '要求：',
    '1. 只返回 JSON，不要附带解释文本。',
    '2. JSON 结构固定为 {"subject":"","body":""}。',
    '3. subject 要明确指出主题和来源，控制在 28 个汉字以内。',
    '4. body 使用纯文本，按以下小节输出：',
    '【页面概览】',
    '【重点内容】',
    '【可执行建议】',
    '【原始链接】',
    '5. 每个小节都写完整句子；如果信息缺失，请明确写“未提供”。',
    extraInstructions,
    '',
    `页面标题：${titleExpression}`,
    `摘要内容：${summaryExpression}`,
    `原始链接：${urlExpression}`
  ]
    .filter(Boolean)
    .join('\\n')
}

const TEMPLATE_DEFINITIONS = [
  {
    key: 'website_email_notify',
    label: '网站采集邮件通知',
    workflowName: '网站采集邮件通知',
    workflowCode: 'WEBSITE_EMAIL_NOTIFY',
    description: '抓取指定网站内容，用 AI 整理成详细邮件摘要，并发送到填写的邮箱地址。',
    category: 'notification',
    buildSchemaFields(createId) {
      return createEmailFields(createId)
    },
    buildGraph(withConfig) {
      return {
        robotBindings: {
          crawlRobotId: null,
          analysisRobotId: null,
          notificationRobotId: null
        },
        nodes: [
          {
            id: 'start_1',
            type: 'start',
            label: '开始',
            description: '流程入口',
            position: { x: 120, y: 180 },
            config: withConfig('start', {})
          },
          {
            id: 'web_crawl_1',
            type: 'web_crawl',
            label: 'Web抓取',
            description: '抓取目标网站正文内容，供后续 AI 整理',
            position: { x: 380, y: 180 },
            config: withConfig('web_crawl', {
              url: '{{ input.target_url }}',
              timeout: 30000,
              headers: {},
              cookies: [],
              extractionRules: [],
              pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
              login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' }
            })
          },
          {
            id: 'ai_generate_email',
            type: 'ai_filter',
            label: 'AI整理邮件',
            description: '把抓取结果整理成更适合阅读和转发的详细邮件内容',
            position: { x: 640, y: 180 },
            config: withConfig('ai_filter', {
              model: '',
              temperature: 0.2,
              systemPrompt: 'You are a workflow assistant that returns strict JSON with subject and body only.',
              userPromptTemplate: detailedEmailPrompt(
                '{{ nodes.web_crawl_1.output.title }}',
                '{{ nodes.web_crawl_1.output.summaryText }}',
                '{{ nodes.web_crawl_1.output.finalUrl }}'
              ),
              outputFormat: 'json'
            })
          },
          {
            id: 'http_send_email',
            type: 'http_request',
            label: 'HTTP请求',
            description: '通过 Resend 把整理后的邮件发送到用户填写的邮箱地址',
            position: { x: 900, y: 180 },
            config: withConfig('http_request', {
              provider: 'resend_email',
              url: 'https://api.resend.com/emails',
              method: 'POST',
              headers: {},
              body: '',
              to: '{{ input.to_email }}',
              subjectTemplate: '{{ nodes.ai_generate_email.output.structured.subject }}',
              textTemplate: '{{ nodes.ai_generate_email.output.structured.body }}',
              htmlTemplate: '',
              timeout: 30000
            })
          },
          {
            id: 'end_1',
            type: 'end',
            label: '结束',
            description: '流程结束',
            position: { x: 1160, y: 180 },
            config: withConfig('end', {})
          }
        ],
        edges: [
          {
            id: 'edge_start_1_web_crawl_1',
            source: 'start_1',
            sourceHandle: 'out',
            target: 'web_crawl_1',
            targetHandle: 'in'
          },
          {
            id: 'edge_web_crawl_1_ai_generate_email',
            source: 'web_crawl_1',
            sourceHandle: 'success',
            target: 'ai_generate_email',
            targetHandle: 'in'
          },
          {
            id: 'edge_ai_generate_email_http_send_email',
            source: 'ai_generate_email',
            sourceHandle: 'success',
            target: 'http_send_email',
            targetHandle: 'in'
          },
          {
            id: 'edge_http_send_email_end_1',
            source: 'http_send_email',
            sourceHandle: 'success',
            target: 'end_1',
            targetHandle: 'in'
          }
        ]
      }
    }
  },
  {
    key: 'website_alert_email',
    label: '重点内容告警邮件',
    workflowName: '重点内容告警邮件',
    workflowCode: 'WEBSITE_ALERT_EMAIL',
    description: '抓取指定网站内容，用 AI 判断是否命中关注主题，命中后发送详细告警邮件。',
    category: 'monitor',
    buildSchemaFields(createId) {
      return createEmailFields(createId, [
        createField(createId, 'alert_topic', '关注主题', '输入你想重点监控的关键词、事件或主题')
      ])
    },
    buildGraph(withConfig) {
      return {
        robotBindings: {
          crawlRobotId: null,
          analysisRobotId: null,
          notificationRobotId: null
        },
        nodes: [
          {
            id: 'start_1',
            type: 'start',
            label: '开始',
            description: '流程入口',
            position: { x: 120, y: 220 },
            config: withConfig('start', {})
          },
          {
            id: 'web_crawl_1',
            type: 'web_crawl',
            label: 'Web抓取',
            description: '抓取目标网站内容',
            position: { x: 360, y: 220 },
            config: withConfig('web_crawl', {
              url: '{{ input.target_url }}',
              timeout: 30000,
              headers: {},
              cookies: [],
              extractionRules: [],
              pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
              login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' }
            })
          },
          {
            id: 'ai_detect_alert',
            type: 'ai_filter',
            label: 'AI判断告警',
            description: '判断网页内容是否命中关注主题，并生成告警邮件文案',
            position: { x: 620, y: 220 },
            config: withConfig('ai_filter', {
              model: '',
              temperature: 0.2,
              systemPrompt: 'You are a workflow assistant that returns strict JSON only.',
              userPromptTemplate: [
                '请判断以下网页内容是否与关注主题高度相关，并输出 JSON。',
                'JSON 结构固定为 {"matched":true,"reason":"","subject":"","body":""}。',
                'matched 只能是 true 或 false。',
                'reason 请用一句话说明判断依据。',
                'subject 和 body 只在 matched=true 时写成详细告警邮件；matched=false 时写成简短未命中说明。',
                'body 使用纯文本，按以下小节输出：',
                '【告警结论】',
                '【命中依据】',
                '【重点内容】',
                '【建议动作】',
                '【原始链接】',
                '',
                '关注主题：{{ input.alert_topic }}',
                '页面标题：{{ nodes.web_crawl_1.output.title }}',
                '摘要内容：{{ nodes.web_crawl_1.output.summaryText }}',
                '原始链接：{{ nodes.web_crawl_1.output.finalUrl }}'
              ].join('\\n'),
              outputFormat: 'json'
            })
          },
          {
            id: 'condition_alert_match',
            type: 'condition',
            label: '命中判断',
            description: '只有命中关注主题时才继续发送邮件',
            position: { x: 860, y: 220 },
            config: withConfig('condition', {
              leftPath: 'nodes.ai_detect_alert.output.structured.matched',
              operator: 'equals',
              rightValue: true,
              rightPath: ''
            })
          },
          {
            id: 'http_send_email',
            type: 'http_request',
            label: 'HTTP请求',
            description: '命中主题后，通过 Resend 发送详细告警邮件',
            position: { x: 1120, y: 140 },
            config: withConfig('http_request', {
              provider: 'resend_email',
              url: 'https://api.resend.com/emails',
              method: 'POST',
              headers: {},
              body: '',
              to: '{{ input.to_email }}',
              subjectTemplate: '{{ nodes.ai_detect_alert.output.structured.subject }}',
              textTemplate: '{{ nodes.ai_detect_alert.output.structured.body }}',
              htmlTemplate: '',
              timeout: 30000
            })
          },
          {
            id: 'end_alert_sent',
            type: 'end',
            label: '结束',
            description: '邮件已发送',
            position: { x: 1380, y: 140 },
            config: withConfig('end', {})
          },
          {
            id: 'end_alert_skipped',
            type: 'end',
            label: '结束',
            description: '未命中主题，流程结束',
            position: { x: 1120, y: 320 },
            config: withConfig('end', {})
          }
        ],
        edges: [
          {
            id: 'edge_start_1_web_crawl_1',
            source: 'start_1',
            sourceHandle: 'out',
            target: 'web_crawl_1',
            targetHandle: 'in'
          },
          {
            id: 'edge_web_crawl_1_ai_detect_alert',
            source: 'web_crawl_1',
            sourceHandle: 'success',
            target: 'ai_detect_alert',
            targetHandle: 'in'
          },
          {
            id: 'edge_ai_detect_alert_condition_alert_match',
            source: 'ai_detect_alert',
            sourceHandle: 'success',
            target: 'condition_alert_match',
            targetHandle: 'in'
          },
          {
            id: 'edge_condition_alert_match_http_send_email',
            source: 'condition_alert_match',
            sourceHandle: 'true',
            target: 'http_send_email',
            targetHandle: 'in'
          },
          {
            id: 'edge_http_send_email_end_alert_sent',
            source: 'http_send_email',
            sourceHandle: 'success',
            target: 'end_alert_sent',
            targetHandle: 'in'
          },
          {
            id: 'edge_condition_alert_match_end_alert_skipped',
            source: 'condition_alert_match',
            sourceHandle: 'false',
            target: 'end_alert_skipped',
            targetHandle: 'in'
          }
        ]
      }
    }
  },
  {
    key: 'dual_site_summary_email',
    label: '双站点汇总邮件',
    workflowName: '双站点汇总邮件',
    workflowCode: 'DUAL_SITE_SUMMARY_EMAIL',
    description: '并行抓取两个网址，汇总成一封详细对比邮件后发送到填写的邮箱地址。',
    category: 'notification',
    buildSchemaFields(createId) {
      return [
        createField(createId, 'target_url_primary', '网址一', '输入第一个要抓取的网站地址', {
          format: 'url'
        }),
        createField(createId, 'target_url_secondary', '网址二', '输入第二个要抓取的网站地址', {
          format: 'url'
        }),
        createField(createId, 'to_email', '收件邮箱', '汇总后的对比邮件会发送到这个邮箱地址', {
          format: 'email'
        })
      ]
    },
    buildGraph(withConfig) {
      return {
        robotBindings: {
          crawlRobotId: null,
          analysisRobotId: null,
          notificationRobotId: null
        },
        nodes: [
          {
            id: 'start_1',
            type: 'start',
            label: '开始',
            description: '流程入口',
            position: { x: 120, y: 280 },
            config: withConfig('start', {})
          },
          {
            id: 'parallel_sources',
            type: 'parallel_split',
            label: '并行抓取',
            description: '同时抓取两个来源，加快汇总速度',
            position: { x: 340, y: 280 },
            config: withConfig('parallel_split', {
              branches: ['primary', 'secondary']
            })
          },
          {
            id: 'web_crawl_primary',
            type: 'web_crawl',
            label: '抓取网址一',
            description: '抓取第一个来源的网站内容',
            position: { x: 620, y: 160 },
            config: withConfig('web_crawl', {
              url: '{{ input.target_url_primary }}',
              timeout: 30000,
              headers: {},
              cookies: [],
              extractionRules: [],
              pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
              login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' }
            })
          },
          {
            id: 'web_crawl_secondary',
            type: 'web_crawl',
            label: '抓取网址二',
            description: '抓取第二个来源的网站内容',
            position: { x: 620, y: 400 },
            config: withConfig('web_crawl', {
              url: '{{ input.target_url_secondary }}',
              timeout: 30000,
              headers: {},
              cookies: [],
              extractionRules: [],
              pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
              login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' }
            })
          },
          {
            id: 'merge_summary',
            type: 'merge',
            label: '汇总结果',
            description: '等待两个来源都完成后，再进行统一整理',
            position: { x: 900, y: 280 },
            config: withConfig('merge', {
              strategy: 'collect'
            })
          },
          {
            id: 'ai_generate_email',
            type: 'ai_filter',
            label: 'AI整理汇总',
            description: '把两个来源的内容整理成一封对比汇总邮件',
            position: { x: 1160, y: 280 },
            config: withConfig('ai_filter', {
              model: '',
              temperature: 0.2,
              systemPrompt: 'You are a workflow assistant that returns strict JSON with subject and body only.',
              userPromptTemplate: [
                '请根据两个来源的抓取结果，生成一封详细的对比汇总邮件。',
                '只返回 JSON，结构固定为 {"subject":"","body":""}。',
                'body 使用纯文本，并按以下小节输出：',
                '【整体概览】',
                '【来源一重点】',
                '【来源二重点】',
                '【差异与趋势】',
                '【建议动作】',
                '【原始链接】',
                '',
                '来源一标题：{{ nodes.merge_summary.output.branches.primary.title }}',
                '来源一摘要：{{ nodes.merge_summary.output.branches.primary.summaryText }}',
                '来源一链接：{{ nodes.merge_summary.output.branches.primary.finalUrl }}',
                '',
                '来源二标题：{{ nodes.merge_summary.output.branches.secondary.title }}',
                '来源二摘要：{{ nodes.merge_summary.output.branches.secondary.summaryText }}',
                '来源二链接：{{ nodes.merge_summary.output.branches.secondary.finalUrl }}'
              ].join('\\n'),
              outputFormat: 'json'
            })
          },
          {
            id: 'http_send_email',
            type: 'http_request',
            label: 'HTTP请求',
            description: '通过 Resend 发送双站点汇总邮件',
            position: { x: 1420, y: 280 },
            config: withConfig('http_request', {
              provider: 'resend_email',
              url: 'https://api.resend.com/emails',
              method: 'POST',
              headers: {},
              body: '',
              to: '{{ input.to_email }}',
              subjectTemplate: '{{ nodes.ai_generate_email.output.structured.subject }}',
              textTemplate: '{{ nodes.ai_generate_email.output.structured.body }}',
              htmlTemplate: '',
              timeout: 30000
            })
          },
          {
            id: 'end_1',
            type: 'end',
            label: '结束',
            description: '流程结束',
            position: { x: 1680, y: 280 },
            config: withConfig('end', {})
          }
        ],
        edges: [
          {
            id: 'edge_start_1_parallel_sources',
            source: 'start_1',
            sourceHandle: 'out',
            target: 'parallel_sources',
            targetHandle: 'in'
          },
          {
            id: 'edge_parallel_sources_primary',
            source: 'parallel_sources',
            sourceHandle: 'primary',
            target: 'web_crawl_primary',
            targetHandle: 'in'
          },
          {
            id: 'edge_parallel_sources_secondary',
            source: 'parallel_sources',
            sourceHandle: 'secondary',
            target: 'web_crawl_secondary',
            targetHandle: 'in'
          },
          {
            id: 'edge_web_crawl_primary_merge_summary',
            source: 'web_crawl_primary',
            sourceHandle: 'success',
            target: 'merge_summary',
            targetHandle: 'in'
          },
          {
            id: 'edge_web_crawl_secondary_merge_summary',
            source: 'web_crawl_secondary',
            sourceHandle: 'success',
            target: 'merge_summary',
            targetHandle: 'in'
          },
          {
            id: 'edge_merge_summary_ai_generate_email',
            source: 'merge_summary',
            sourceHandle: 'out',
            target: 'ai_generate_email',
            targetHandle: 'in'
          },
          {
            id: 'edge_ai_generate_email_http_send_email',
            source: 'ai_generate_email',
            sourceHandle: 'success',
            target: 'http_send_email',
            targetHandle: 'in'
          },
          {
            id: 'edge_http_send_email_end_1',
            source: 'http_send_email',
            sourceHandle: 'success',
            target: 'end_1',
            targetHandle: 'in'
          }
        ]
      }
    }
  },
  {
    key: 'website_webhook_sync',
    label: '网站采集同步到接口',
    workflowName: '网站采集同步到接口',
    workflowCode: 'WEBSITE_WEBHOOK_SYNC',
    description: '抓取指定网站内容，提取关键信息后通过 HTTP 请求同步到业务接口。',
    category: 'sync',
    buildSchemaFields(createId) {
      return createWebhookFields(createId)
    },
    buildGraph(withConfig) {
      return {
        robotBindings: {
          crawlRobotId: null,
          analysisRobotId: null,
          notificationRobotId: null
        },
        nodes: [
          {
            id: 'start_1',
            type: 'start',
            label: '开始',
            description: '流程入口',
            position: { x: 120, y: 180 },
            config: withConfig('start', {})
          },
          {
            id: 'web_crawl_1',
            type: 'web_crawl',
            label: 'Web抓取',
            description: '抓取目标网站内容',
            position: { x: 380, y: 180 },
            config: withConfig('web_crawl', {
              url: '{{ input.target_url }}',
              timeout: 30000,
              headers: {},
              cookies: [],
              extractionRules: [],
              pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
              login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' }
            })
          },
          {
            id: 'transform_payload',
            type: 'transform',
            label: '整理字段',
            description: '提取常用字段，整理成统一的同步载荷',
            position: { x: 640, y: 180 },
            config: withConfig('transform', {
              mappings: {
                source_url: 'input.target_url',
                final_url: 'nodes.web_crawl_1.output.finalUrl',
                page_title: 'nodes.web_crawl_1.output.title',
                summary_text: 'nodes.web_crawl_1.output.summaryText',
                total_count: 'nodes.web_crawl_1.output.totalCount'
              }
            })
          },
          {
            id: 'http_push_result',
            type: 'http_request',
            label: 'HTTP请求',
            description: '把整理好的字段同步到业务接口',
            position: { x: 900, y: 180 },
            config: withConfig('http_request', {
              provider: '',
              url: '{{ input.webhook_url }}',
              method: 'POST',
              headers: {
                'X-Workflow-Token': '{{ input.webhook_token }}'
              },
              body: `{
  "source": "project-gl",
  "source_url": "{{ nodes.transform_payload.output.source_url }}",
  "final_url": "{{ nodes.transform_payload.output.final_url }}",
  "page_title": "{{ nodes.transform_payload.output.page_title }}",
  "summary_text": "{{ nodes.transform_payload.output.summary_text }}",
  "total_count": "{{ nodes.transform_payload.output.total_count }}"
}`,
              to: '',
              subjectTemplate: '',
              textTemplate: '',
              htmlTemplate: '',
              timeout: 30000
            })
          },
          {
            id: 'end_1',
            type: 'end',
            label: '结束',
            description: '流程结束',
            position: { x: 1160, y: 180 },
            config: withConfig('end', {})
          }
        ],
        edges: [
          {
            id: 'edge_start_1_web_crawl_1',
            source: 'start_1',
            sourceHandle: 'out',
            target: 'web_crawl_1',
            targetHandle: 'in'
          },
          {
            id: 'edge_web_crawl_1_transform_payload',
            source: 'web_crawl_1',
            sourceHandle: 'success',
            target: 'transform_payload',
            targetHandle: 'in'
          },
          {
            id: 'edge_transform_payload_http_push_result',
            source: 'transform_payload',
            sourceHandle: 'success',
            target: 'http_push_result',
            targetHandle: 'in'
          },
          {
            id: 'edge_http_push_result_end_1',
            source: 'http_push_result',
            sourceHandle: 'success',
            target: 'end_1',
            targetHandle: 'in'
          }
        ]
      }
    }
  }
]

function resolveTemplate(templateKey) {
  return TEMPLATE_DEFINITIONS.find((template) => template.key === templateKey) || TEMPLATE_DEFINITIONS[0]
}

export function getOfficialDemoTemplateOptions() {
  return TEMPLATE_DEFINITIONS.map((template) => ({
    key: template.key,
    label: template.label,
    workflowName: template.workflowName,
    workflowCode: template.workflowCode,
    description: template.description,
    category: template.category
  }))
}

export function getOfficialDemoTemplateMeta(templateKey = DEFAULT_OFFICIAL_TEMPLATE_KEY) {
  const template = resolveTemplate(templateKey)
  return {
    key: template.key,
    label: template.label,
    workflowName: template.workflowName,
    workflowCode: template.workflowCode,
    description: template.description,
    category: template.category
  }
}

export function findOfficialDemoTemplateByWorkflowCode(workflowCode) {
  return TEMPLATE_DEFINITIONS.find((template) => template.workflowCode === workflowCode) || null
}

export function createOfficialDemoTemplateSchemaFields(
  templateKey = DEFAULT_OFFICIAL_TEMPLATE_KEY,
  createId = defaultCreateId
) {
  return resolveTemplate(templateKey).buildSchemaFields(createId)
}

export function createOfficialDemoTemplateGraph(
  templateKey = DEFAULT_OFFICIAL_TEMPLATE_KEY,
  { normalizeNodeConfig } = {}
) {
  const withConfig = (type, config) => {
    if (typeof normalizeNodeConfig === 'function') {
      return normalizeNodeConfig(type, config)
    }
    return cloneConfig(config)
  }

  return resolveTemplate(templateKey).buildGraph(withConfig)
}
