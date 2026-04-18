<template>
  <div class="workflow-designer-page">
    <div class="designer-shell">
      <div class="designer-header">
        <div class="designer-title-group">
          <div class="eyebrow">Workflow Studio V2</div>
          <h2>{{ pageTitle }}</h2>
          <p>顶部 AI 生成，中间自由画布，右侧结构化 Inspector。草稿可调试，发布后按图执行。</p>
        </div>
        <div class="designer-header-actions">
          <div class="draft-indicator" :data-state="draftState">{{ draftStateText }}</div>
          <el-tag :type="statusTagType" effect="plain">{{ statusText }}</el-tag>
          <el-button @click="handleBack">返回列表</el-button>
          <el-button v-if="!isViewMode" :loading="saving" @click="handleSave">保存草稿</el-button>
          <el-button
            v-if="!isViewMode"
            type="success"
            :loading="publishing"
            :disabled="publishing || validationIssues.length > 0"
            @click="handlePublish"
          >
            发布流程
          </el-button>
        </div>
      </div>

      <section class="ai-strip">
        <div class="ai-strip-copy">
          <div class="ai-badge">AI Draft</div>
          <h3>先生成一个初版图，再在画布里继续打磨</h3>
          <p>AI 只负责生成初始流程图和输入 Schema，不会直接发布。</p>
        </div>
        <div class="ai-strip-input">
          <el-input
            v-model="assistantPrompt"
            :disabled="drafting || isViewMode"
            maxlength="600"
            placeholder="例如：抓取指定页面，判断金额是否超标，超标则并行通知财务和风控，否则整理结果后结束"
            @keyup.enter.exact.prevent="handleGenerateDraft"
          />
          <el-button
            type="primary"
            :loading="drafting"
            :disabled="isViewMode"
            @click="handleGenerateDraft"
          >
            生成初始流程图
          </el-button>
        </div>
      </section>

      <div class="designer-content">
        <section class="canvas-stage">
          <div class="stage-toolbar">
            <div class="toolbar-left">
              <div class="toolbar-group">
                <span class="toolbar-label">快捷新增</span>
                <el-select v-model="insertNodeType" :disabled="isViewMode" style="width: 220px">
                  <el-option
                    v-for="item in creatableNodeOptions"
                    :key="item.type"
                    :label="item.name"
                    :value="item.type"
                  />
                </el-select>
                <el-button type="primary" :disabled="isViewMode" @click="handleAddNode">
                  {{ selectedEdge ? '插入到连线' : selectedNode ? '添加到当前节点后' : '新增节点' }}
                </el-button>
              </div>
              <div class="toolbar-group">
                <el-button :disabled="graphNodes.length === 0" @click="handleAutoLayout">自动布局</el-button>
                <el-button :disabled="graphNodes.length === 0" @click="handleFitView">适配视图</el-button>
                <el-button
                  v-if="selectedNode && !isViewMode"
                  type="danger"
                  plain
                  @click="removeSelectedNode"
                >
                  删除节点
                </el-button>
                <el-button
                  v-if="selectedEdge && !isViewMode"
                  type="danger"
                  plain
                  @click="removeSelectedEdge"
                >
                  删除连线
                </el-button>
              </div>
            </div>
            <div class="toolbar-right">
              <span v-if="selectedEdge" class="toolbar-hint">
                当前已选中连线，新增节点会自动插入中间
              </span>
              <span v-else-if="selectedNode" class="toolbar-hint">
                当前已选中节点，新增节点会默认接在它后面
              </span>
              <span v-else class="toolbar-hint">
                直接拖动画布、拖拽节点、连线分支即可编排流程
              </span>
            </div>
          </div>

          <div class="stage-canvas">
            <VueFlow
              :nodes="canvasNodes"
              :edges="canvasEdges"
              :default-viewport="{ zoom: 0.9 }"
              :min-zoom="0.2"
              :max-zoom="1.6"
              :nodes-draggable="!isViewMode"
              :nodes-connectable="!isViewMode"
              :elements-selectable="true"
              class="workflow-flow"
              @connect="handleConnect"
              @node-click="handleNodeClick"
              @edge-click="handleEdgeClick"
              @pane-click="clearSelection"
              @node-drag-stop="handleNodeDragStop"
            >
              <Background :gap="20" pattern-color="rgba(31, 41, 55, 0.08)" />

              <template #node-designer="{ data, selected }">
                <div class="designer-node" :class="{ 'is-selected': selected }" :style="{ '--node-accent': data.color }">
                  <div class="node-card-head">
                    <span class="node-kind">{{ data.definition.name }}</span>
                    <span class="node-id">{{ data.id }}</span>
                  </div>
                  <div class="node-card-title">{{ data.label }}</div>
                  <div class="node-card-desc">
                    {{ data.description || data.definition.description || '点击右侧 Inspector 配置节点参数' }}
                  </div>
                  <div class="node-card-foot">
                    <span>{{ data.definition.category }}</span>
                    <span>{{ data.outputHandles.length }} out</span>
                  </div>

                  <template v-for="(handle, index) in data.inputHandles" :key="`in-${data.id}-${handle.id}`">
                    <div class="handle-wrap handle-wrap-left" :style="handlePositionStyle(index, data.inputHandles.length)">
                      <Handle :id="handle.id" type="target" :position="Position.Left" class="flow-handle" />
                      <span>{{ handle.label }}</span>
                    </div>
                  </template>

                  <template v-for="(handle, index) in data.outputHandles" :key="`out-${data.id}-${handle.id}`">
                    <div class="handle-wrap handle-wrap-right" :style="handlePositionStyle(index, data.outputHandles.length)">
                      <span>{{ handle.label }}</span>
                      <Handle :id="handle.id" type="source" :position="Position.Right" class="flow-handle" />
                    </div>
                  </template>
                </div>
              </template>
            </VueFlow>
          </div>
        </section>

        <aside class="inspector-stage">
          <div class="inspector-panel">
            <div class="panel-header">
              <div>
                <div class="panel-kicker">Inspector</div>
                <h3>{{ selectedNode ? '节点配置' : '工作流配置' }}</h3>
              </div>
              <div class="panel-meta">
                <span>{{ graphNodes.length }} 节点</span>
                <span>{{ graphEdges.length }} 连线</span>
              </div>
            </div>

            <div v-if="selectedNode" class="panel-body scrollable">
              <el-form label-position="top" class="compact-form">
                <el-form-item label="节点名称">
                  <el-input v-model="selectedNode.label" :disabled="isViewMode" />
                </el-form-item>

                <el-form-item label="节点类型">
                  <el-select v-model="selectedNode.type" :disabled="isViewMode" @change="handleSelectedNodeTypeChange">
                    <el-option
                      v-for="item in creatableNodeOptions"
                      :key="item.type"
                      :label="item.name"
                      :value="item.type"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="节点说明">
                  <el-input
                    v-model="selectedNode.description"
                    :disabled="isViewMode"
                    type="textarea"
                    :rows="3"
                    placeholder="告诉团队这个节点在做什么"
                  />
                </el-form-item>

                <template v-if="selectedNode.type === 'http_request'">
                  <el-form-item label="URL">
                    <el-input v-model="selectedNode.config.url" :disabled="isViewMode" placeholder="https://example.com/api" />
                  </el-form-item>
                  <div class="two-col-grid">
                    <el-form-item label="Method">
                      <el-select v-model="selectedNode.config.method" :disabled="isViewMode">
                        <el-option label="GET" value="GET" />
                        <el-option label="POST" value="POST" />
                        <el-option label="PUT" value="PUT" />
                        <el-option label="DELETE" value="DELETE" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="超时(ms)">
                      <el-input-number v-model="selectedNode.config.timeout" :disabled="isViewMode" :min="1000" :step="1000" controls-position="right" style="width: 100%" />
                    </el-form-item>
                  </div>
                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>Headers</strong>
                      <el-button link :disabled="isViewMode" @click="addHeaderRow">新增</el-button>
                    </div>
                    <div v-if="headerRows.length === 0" class="empty-inline">暂无请求头</div>
                    <div v-for="(row, index) in headerRows" :key="row.id" class="row-editor">
                      <el-input v-model="row.key" :disabled="isViewMode" placeholder="Header" @input="syncHeaderRows" />
                      <el-input v-model="row.value" :disabled="isViewMode" placeholder="Value" @input="syncHeaderRows" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeHeaderRow(index)">删除</el-button>
                    </div>
                  </div>
                  <el-form-item label="Body">
                    <el-input
                      v-model="selectedNode.config.body"
                      :disabled="isViewMode"
                      type="textarea"
                      :rows="4"
                      placeholder="支持模板变量，例如 {{ input.url }}"
                    />
                  </el-form-item>
                </template>

                <template v-else-if="selectedNode.type === 'web_crawl'">
                  <el-form-item label="入口 URL">
                    <el-input v-model="selectedNode.config.url" :disabled="isViewMode" placeholder="https://example.com/list" />
                  </el-form-item>
                  <div class="two-col-grid">
                    <el-form-item label="超时(ms)">
                      <el-input-number v-model="selectedNode.config.timeout" :disabled="isViewMode" :min="1000" :step="1000" controls-position="right" style="width: 100%" />
                    </el-form-item>
                    <el-form-item label="分页最大页数">
                      <el-input-number v-model="selectedNode.config.pagination.maxPages" :disabled="isViewMode" :min="1" :step="1" controls-position="right" style="width: 100%" />
                    </el-form-item>
                  </div>

                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>Headers</strong>
                      <el-button link :disabled="isViewMode" @click="addHeaderRow">新增</el-button>
                    </div>
                    <div v-if="headerRows.length === 0" class="empty-inline">暂无请求头</div>
                    <div v-for="(row, index) in headerRows" :key="row.id" class="row-editor">
                      <el-input v-model="row.key" :disabled="isViewMode" placeholder="Header" @input="syncHeaderRows" />
                      <el-input v-model="row.value" :disabled="isViewMode" placeholder="Value" @input="syncHeaderRows" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeHeaderRow(index)">删除</el-button>
                    </div>
                  </div>

                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>Cookies</strong>
                      <el-button link :disabled="isViewMode" @click="addCookieRow">新增</el-button>
                    </div>
                    <div v-if="cookieRows.length === 0" class="empty-inline">暂无 Cookie</div>
                    <div v-for="(row, index) in cookieRows" :key="row.id" class="row-editor">
                      <el-input v-model="row.key" :disabled="isViewMode" placeholder="Cookie 名称" @input="syncCookieRows" />
                      <el-input v-model="row.value" :disabled="isViewMode" placeholder="Cookie 值" @input="syncCookieRows" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeCookieRow(index)">删除</el-button>
                    </div>
                  </div>

                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>抽取规则</strong>
                      <el-button link :disabled="isViewMode" @click="addExtractionRule">新增</el-button>
                    </div>
                    <div v-if="extractionRuleRows.length === 0" class="empty-inline">暂无抽取规则</div>
                    <div v-for="(row, index) in extractionRuleRows" :key="row.id" class="rule-editor">
                      <el-input v-model="row.field" :disabled="isViewMode" placeholder="字段名" @input="syncExtractionRules" />
                      <el-input v-model="row.selector" :disabled="isViewMode" placeholder="CSS Selector" @input="syncExtractionRules" />
                      <el-select v-model="row.type" :disabled="isViewMode" @change="syncExtractionRules">
                        <el-option label="text" value="text" />
                        <el-option label="html" value="html" />
                        <el-option label="attr" value="attr" />
                      </el-select>
                      <el-input v-model="row.attr" :disabled="isViewMode" placeholder="attr 名称(可选)" @input="syncExtractionRules" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeExtractionRule(index)">删除</el-button>
                    </div>
                  </div>

                  <div class="two-col-grid">
                    <el-form-item label="分页选择器">
                      <el-input v-model="selectedNode.config.pagination.nextSelector" :disabled="isViewMode" placeholder=".next" />
                    </el-form-item>
                    <el-form-item label="分页参数名">
                      <el-input v-model="selectedNode.config.pagination.pageParam" :disabled="isViewMode" placeholder="page" />
                    </el-form-item>
                  </div>

                  <div class="two-col-grid">
                    <el-form-item label="登录地址">
                      <el-input v-model="selectedNode.config.login.loginUrl" :disabled="isViewMode" placeholder="https://example.com/login" />
                    </el-form-item>
                    <el-form-item label="提交按钮选择器">
                      <el-input v-model="selectedNode.config.login.submitSelector" :disabled="isViewMode" placeholder="button[type=submit]" />
                    </el-form-item>
                  </div>
                  <div class="two-col-grid">
                    <el-form-item label="用户名选择器">
                      <el-input v-model="selectedNode.config.login.usernameSelector" :disabled="isViewMode" placeholder="input[name=username]" />
                    </el-form-item>
                    <el-form-item label="密码选择器">
                      <el-input v-model="selectedNode.config.login.passwordSelector" :disabled="isViewMode" placeholder="input[type=password]" />
                    </el-form-item>
                  </div>
                </template>

                <template v-else-if="selectedNode.type === 'ai_filter'">
                  <div class="two-col-grid">
                    <el-form-item label="模型">
                      <el-input v-model="selectedNode.config.model" :disabled="isViewMode" placeholder="gpt-4.1 / deepseek / qwen..." />
                    </el-form-item>
                    <el-form-item label="温度">
                      <el-input-number v-model="selectedNode.config.temperature" :disabled="isViewMode" :min="0" :max="2" :step="0.1" controls-position="right" style="width: 100%" />
                    </el-form-item>
                  </div>
                  <el-form-item label="系统提示词">
                    <el-input v-model="selectedNode.config.systemPrompt" :disabled="isViewMode" type="textarea" :rows="4" />
                  </el-form-item>
                  <el-form-item label="用户提示模板">
                    <el-input
                      v-model="selectedNode.config.userPromptTemplate"
                      :disabled="isViewMode"
                      type="textarea"
                      :rows="5"
                      placeholder="支持模板变量，例如 {{ nodes.web_crawl_1.output.summaryText }}"
                    />
                  </el-form-item>
                  <el-form-item label="输出格式">
                    <el-select v-model="selectedNode.config.outputFormat" :disabled="isViewMode">
                      <el-option label="text" value="text" />
                      <el-option label="json" value="json" />
                    </el-select>
                  </el-form-item>
                </template>

                <template v-else-if="selectedNode.type === 'condition'">
                  <el-form-item label="左值路径">
                    <el-input v-model="selectedNode.config.leftPath" :disabled="isViewMode" placeholder="nodes.http_request_1.output.statusCode" />
                  </el-form-item>
                  <div class="two-col-grid">
                    <el-form-item label="操作符">
                      <el-select v-model="selectedNode.config.operator" :disabled="isViewMode">
                        <el-option label="equals" value="equals" />
                        <el-option label="contains" value="contains" />
                        <el-option label="not_empty" value="not_empty" />
                        <el-option label="greater_than" value="greater_than" />
                        <el-option label="less_than" value="less_than" />
                        <el-option label="exists" value="exists" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="右值">
                      <el-input v-model="selectedNode.config.rightValue" :disabled="isViewMode" placeholder="200" />
                    </el-form-item>
                  </div>
                  <el-form-item label="右值路径(可选)">
                    <el-input v-model="selectedNode.config.rightPath" :disabled="isViewMode" placeholder="input.threshold" />
                  </el-form-item>
                </template>

                <template v-else-if="selectedNode.type === 'parallel_split'">
                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>分支名称</strong>
                      <el-button link :disabled="isViewMode" @click="addBranchRow">新增</el-button>
                    </div>
                    <div v-if="branchRows.length === 0" class="empty-inline">至少需要两个分支</div>
                    <div v-for="(row, index) in branchRows" :key="row.id" class="row-editor">
                      <el-input v-model="row.value" :disabled="isViewMode" placeholder="branch_a" @input="syncBranchRows" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeBranchRow(index)">删除</el-button>
                    </div>
                  </div>
                </template>

                <template v-else-if="selectedNode.type === 'merge'">
                  <el-form-item label="合并策略">
                    <el-select v-model="selectedNode.config.strategy" :disabled="isViewMode">
                      <el-option label="collect" value="collect" />
                      <el-option label="latest" value="latest" />
                    </el-select>
                  </el-form-item>
                </template>

                <template v-else-if="selectedNode.type === 'transform'">
                  <div class="collection-editor">
                    <div class="editor-head">
                      <strong>字段映射</strong>
                      <el-button link :disabled="isViewMode" @click="addMappingRow">新增</el-button>
                    </div>
                    <div v-if="mappingRows.length === 0" class="empty-inline">暂无字段映射</div>
                    <div v-for="(row, index) in mappingRows" :key="row.id" class="row-editor">
                      <el-input v-model="row.key" :disabled="isViewMode" placeholder="输出字段" @input="syncMappingRows" />
                      <el-input v-model="row.value" :disabled="isViewMode" placeholder="来源路径，如 nodes.ai_1.output.content" @input="syncMappingRows" />
                      <el-button link type="danger" :disabled="isViewMode" @click="removeMappingRow(index)">删除</el-button>
                    </div>
                  </div>
                </template>

                <template v-else-if="selectedNode.type === 'notification'">
                  <div class="two-col-grid">
                    <el-form-item label="渠道">
                      <el-select v-model="selectedNode.config.channel" :disabled="isViewMode">
                        <el-option label="email" value="email" />
                        <el-option label="webhook" value="webhook" />
                        <el-option label="sms" value="sms" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="接收人">
                      <el-input v-model="selectedNode.config.recipients" :disabled="isViewMode" placeholder="ops@example.com" />
                    </el-form-item>
                  </div>
                  <el-form-item label="主题">
                    <el-input v-model="selectedNode.config.subject" :disabled="isViewMode" />
                  </el-form-item>
                  <el-form-item label="正文模板">
                    <el-input
                      v-model="selectedNode.config.bodyTemplate"
                      :disabled="isViewMode"
                      type="textarea"
                      :rows="5"
                      placeholder="支持模板变量，例如 {{ nodes.transform_1.output.summary }}"
                    />
                  </el-form-item>
                </template>

                <template v-else>
                  <div class="empty-state-card">当前节点没有额外结构化配置。</div>
                </template>
              </el-form>
            </div>

            <div v-else class="panel-body scrollable">
              <el-form label-position="top" class="compact-form">
                <el-form-item label="流程名称">
                  <el-input v-model="workflowForm.name" :disabled="isViewMode" placeholder="例如：订单舆情巡检流" />
                </el-form-item>
                <el-form-item label="流程编码">
                  <el-input v-model="workflowForm.workflowCode" :disabled="isViewMode" placeholder="WF_ORDER_MONITOR" />
                </el-form-item>
                <div class="two-col-grid">
                  <el-form-item label="流程分类">
                    <el-select v-model="workflowForm.category" :disabled="isViewMode">
                      <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="当前状态">
                    <el-input :model-value="statusText" disabled />
                  </el-form-item>
                </div>
                <el-form-item label="流程描述">
                  <el-input
                    v-model="workflowForm.description"
                    :disabled="isViewMode"
                    type="textarea"
                    :rows="4"
                    placeholder="说明这个流程的业务目标、输入和输出"
                  />
                </el-form-item>
              </el-form>

              <div class="schema-section">
                <div class="section-inline-head">
                  <div>
                    <h4>输入 Schema</h4>
                    <p>任务创建页会按照这里的定义动态生成输入表单。</p>
                  </div>
                  <el-button v-if="!isViewMode" @click="addSchemaField">新增字段</el-button>
                </div>

                <div v-if="inputFields.length === 0" class="empty-state-card">暂无输入字段，可以手动补充或先让 AI 生成。</div>

                <div v-for="(field, index) in inputFields" :key="field.id" class="schema-card">
                  <div class="schema-card-head">
                    <strong>字段 {{ index + 1 }}</strong>
                    <el-button v-if="!isViewMode" link type="danger" @click="removeSchemaField(index)">删除</el-button>
                  </div>
                  <div class="two-col-grid">
                    <el-input v-model="field.name" :disabled="isViewMode" placeholder="字段名，例如 url" />
                    <el-input v-model="field.title" :disabled="isViewMode" placeholder="展示名，例如 目标链接" />
                  </div>
                  <div class="two-col-grid">
                    <el-select v-model="field.type" :disabled="isViewMode">
                      <el-option v-for="item in schemaTypeOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                    <el-input v-model="field.format" :disabled="isViewMode" placeholder="可选：textarea / password / url" />
                  </div>
                  <el-input
                    v-model="field.description"
                    :disabled="isViewMode"
                    type="textarea"
                    :rows="2"
                    placeholder="字段说明"
                  />
                  <div class="two-col-grid">
                    <el-input v-model="field.enumText" :disabled="isViewMode" placeholder="枚举值，逗号分隔" />
                    <el-input v-model="field.defaultValue" :disabled="isViewMode" placeholder="默认值" />
                  </div>
                  <el-switch v-model="field.required" :disabled="isViewMode" inline-prompt active-text="必填" inactive-text="可选" />
                </div>
              </div>
            </div>
          </div>

          <div class="info-panel">
            <div class="panel-header compact">
              <div>
                <div class="panel-kicker">Validation</div>
                <h3>发布前校验</h3>
              </div>
            </div>
            <div class="panel-body">
              <div v-if="validationIssues.length === 0" class="validation-ok">当前草稿通过前端校验，可以保存并发布。</div>
              <ul v-else class="issue-list">
                <li v-for="issue in validationIssues" :key="issue">{{ issue }}</li>
              </ul>
              <ul v-if="assistantWarnings.length > 0" class="warning-list">
                <li v-for="warning in assistantWarnings" :key="warning">{{ warning }}</li>
              </ul>
            </div>
          </div>

          <div class="info-panel">
            <div class="panel-header compact">
              <div>
                <div class="panel-kicker">Debug Run</div>
                <h3>草稿试跑</h3>
              </div>
              <el-button
                v-if="!isViewMode"
                type="primary"
                plain
                size="small"
                :loading="debugging"
                @click="handleDebugRun"
              >
                保存并试跑
              </el-button>
            </div>
            <div class="panel-body scrollable">
              <el-input
                v-if="!isViewMode"
                v-model="debugInputText"
                type="textarea"
                :rows="5"
                placeholder='调试输入 JSON，例如 { "url": "https://example.com" }'
              />

              <div v-if="debugRuns.length > 0" class="debug-run-tabs">
                <button
                  v-for="run in debugRuns.slice(0, 5)"
                  :key="run.runId"
                  class="debug-run-chip"
                  :class="{ active: latestDebugRun?.runId === run.runId }"
                  @click="loadDebugRunDetail(run.runId)"
                >
                  {{ run.runId }}
                </button>
              </div>

              <div v-if="latestDebugRun" class="debug-summary">
                <div class="summary-row">
                  <span>状态</span>
                  <el-tag :type="debugStatusType(latestDebugRun.status)" size="small">{{ latestDebugRun.status }}</el-tag>
                </div>
                <div class="summary-row">
                  <span>进度</span>
                  <strong>{{ latestDebugRun.progress || 0 }}%</strong>
                </div>
                <div class="summary-row">
                  <span>耗时</span>
                  <strong>{{ latestDebugRun.duration || 0 }}s</strong>
                </div>
                <div v-if="latestDebugRun.errorMessage" class="debug-error">{{ latestDebugRun.errorMessage }}</div>
                <div v-if="latestDebugRun.stepRuns?.length" class="step-list">
                  <div v-for="step in latestDebugRun.stepRuns" :key="step.stepRunId" class="step-card">
                    <div class="step-head">
                      <strong>{{ step.nodeLabel || step.nodeId }}</strong>
                      <el-tag size="small" :type="debugStatusType(step.status)">{{ step.status }}</el-tag>
                    </div>
                    <div class="step-meta">{{ step.nodeType }} · {{ step.branchKey || 'main' }}</div>
                    <div v-if="step.errorMessage" class="step-error">{{ step.errorMessage }}</div>
                  </div>
                </div>
              </div>

              <div v-else class="empty-state-card">还没有草稿试跑记录。</div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import dagre from 'dagre'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Background } from '@vue-flow/background'
import { Handle, MarkerType, Position, VueFlow, useVueFlow } from '@vue-flow/core'
import {
  createWorkflow,
  createWorkflowDebugRun,
  createWorkflowDraft,
  getAllNodeTypes,
  getWorkflowById,
  getWorkflowDebugRunDetail,
  getWorkflowDebugRuns,
  publishWorkflow,
  updateWorkflow
} from '../../api/workflow.js'

const route = useRoute()
const router = useRouter()
const { fitView } = useVueFlow()

const workflowId = computed(() => (route.query.id ? Number(route.query.id) : null))
const mode = computed(() => String(route.query.mode || (workflowId.value ? 'edit' : 'create')))
const isViewMode = computed(() => mode.value === 'view')

const pageTitle = computed(() => {
  if (isViewMode.value) return '查看流程设计器'
  if (mode.value === 'ai') return 'AI 生成流程设计器'
  return workflowId.value ? '编辑流程设计器' : '新建流程设计器'
})

const categoryOptions = [
  { label: '数据采集', value: 'data_collection' },
  { label: '任务分析', value: 'analysis' },
  { label: '报表生成', value: 'report' },
  { label: '数据同步', value: 'sync' },
  { label: '审批流转', value: 'approval' },
  { label: '巡检监控', value: 'monitor' },
  { label: '通知推送', value: 'notification' },
  { label: '文件处理', value: 'file' },
  { label: '清洗加工', value: 'transform' },
  { label: '其他', value: 'other' }
]

const schemaTypeOptions = ['string', 'integer', 'number', 'boolean', 'array', 'object']

const NODE_LIBRARY = {
  start: {
    type: 'start',
    name: '开始',
    color: '#1b7f5f',
    category: '基础',
    description: '流程入口',
    defaultConfig: {}
  },
  http_request: {
    type: 'http_request',
    name: 'HTTP 请求',
    color: '#2f6bff',
    category: '采集',
    description: '请求接口或网页',
    defaultConfig: { url: '', method: 'GET', headers: {}, body: '', timeout: 30000 }
  },
  web_crawl: {
    type: 'web_crawl',
    name: '网页采集',
    color: '#0f9d8a',
    category: '采集',
    description: '通过 Spider 服务抓取页面',
    defaultConfig: {
      url: '',
      headers: {},
      cookies: [],
      extractionRules: [],
      pagination: { maxPages: 1, nextSelector: '', pageParam: '' },
      login: { loginUrl: '', submitSelector: '', usernameSelector: '', passwordSelector: '' },
      timeout: 30000
    }
  },
  ai_filter: {
    type: 'ai_filter',
    name: 'AI 处理',
    color: '#7c4dff',
    category: 'AI',
    description: '同步调用模型进行分析',
    defaultConfig: {
      model: '',
      temperature: 0.2,
      systemPrompt: '',
      userPromptTemplate: '',
      outputFormat: 'text'
    }
  },
  condition: {
    type: 'condition',
    name: '条件判断',
    color: '#8a6b2d',
    category: '逻辑',
    description: '根据路径和操作符判断分支',
    defaultConfig: { leftPath: '', operator: 'equals', rightValue: '', rightPath: '' }
  },
  parallel_split: {
    type: 'parallel_split',
    name: '并行分叉',
    color: '#d97706',
    category: '逻辑',
    description: '把一个节点拆成多个并行分支',
    defaultConfig: { branches: ['branch_a', 'branch_b'] }
  },
  merge: {
    type: 'merge',
    name: '汇聚',
    color: '#4b5563',
    category: '逻辑',
    description: '等待多个分支汇合',
    defaultConfig: { strategy: 'collect' }
  },
  transform: {
    type: 'transform',
    name: '字段变换',
    color: '#2563eb',
    category: '处理',
    description: '按字段映射重新输出',
    defaultConfig: { mappings: {} }
  },
  notification: {
    type: 'notification',
    name: '通知',
    color: '#ef476f',
    category: '输出',
    description: '发送消息通知',
    defaultConfig: { channel: 'email', recipients: '', subject: '', bodyTemplate: '' }
  },
  end: {
    type: 'end',
    name: '结束',
    color: '#dc2626',
    category: '基础',
    description: '流程终点',
    defaultConfig: {}
  }
}

const creatableNodeOptions = computed(() =>
  Object.values(NODE_LIBRARY).map((item) => {
    const remote = nodeTypeOptions.value.find((nodeType) => nodeType.type === item.type)
    return {
      ...item,
      name: remote?.name || item.name
    }
  })
)

const workflowForm = reactive({
  name: '',
  workflowCode: '',
  description: '',
  category: 'data_collection',
  status: 'draft'
})

const assistantPrompt = ref('')
const assistantWarnings = ref([])
const saving = ref(false)
const publishing = ref(false)
const drafting = ref(false)
const debugging = ref(false)
const draftState = ref('saved')
const draftStateText = computed(() => {
  const map = {
    saved: '草稿已同步',
    dirty: '草稿待保存',
    saving: '正在自动保存',
    error: '自动保存失败'
  }
  return map[draftState.value] || '草稿待保存'
})

const statusText = computed(() => {
  const map = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档'
  }
  return map[workflowForm.status] || workflowForm.status || '-'
})

const statusTagType = computed(() => {
  const map = {
    draft: 'info',
    published: 'success',
    archived: 'warning'
  }
  return map[workflowForm.status] || 'info'
})

const nodeTypeOptions = ref([])
const insertNodeType = ref('http_request')
const inputFields = ref([])
const graphNodes = ref([])
const graphEdges = ref([])
const selectedNodeId = ref('')
const selectedEdgeId = ref('')
const headerRows = ref([])
const cookieRows = ref([])
const extractionRuleRows = ref([])
const mappingRows = ref([])
const branchRows = ref([])
const debugInputText = ref('{}')
const debugRuns = ref([])
const latestDebugRun = ref(null)

const selectedNode = computed(() => graphNodes.value.find((node) => node.id === selectedNodeId.value) || null)
const selectedEdge = computed(() => graphEdges.value.find((edge) => edge.id === selectedEdgeId.value) || null)

const canvasNodes = computed(() =>
  graphNodes.value.map((node) => {
    const definition = resolveNodeDefinition(node.type)
    return {
      id: node.id,
      type: 'designer',
      position: node.position,
      data: {
        id: node.id,
        label: node.label,
        description: node.description,
        color: definition.color,
        definition,
        inputHandles: getInputHandles(node),
        outputHandles: getOutputHandles(node)
      }
    }
  })
)

const canvasEdges = computed(() =>
  graphEdges.value.map((edge) => {
    const highlighted = selectedNodeId.value
      ? edge.source === selectedNodeId.value || edge.target === selectedNodeId.value
      : selectedEdgeId.value === edge.id
    const stroke = highlighted ? '#ff7a18' : 'rgba(71, 85, 105, 0.58)'
    return {
      ...edge,
      type: 'smoothstep',
      animated: highlighted,
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: stroke,
        width: 18,
        height: 18
      },
      style: {
        stroke,
        strokeWidth: highlighted ? 2.6 : 1.8
      },
      class: highlighted ? 'flow-edge-highlighted' : 'flow-edge-normal'
    }
  })
)

const currentSignature = computed(() =>
  JSON.stringify({
    workflow: {
      name: workflowForm.name,
      workflowCode: workflowForm.workflowCode,
      description: workflowForm.description,
      category: workflowForm.category,
      status: workflowForm.status
    },
    inputFields: inputFields.value,
    graphNodes: graphNodes.value,
    graphEdges: graphEdges.value
  })
)

const validationIssues = computed(() => validateGraph(graphNodes.value, graphEdges.value))

let autoSaveTimer = null
let debugPollingTimer = null
let hydrating = false
let lastSavedSignature = ''

watch(selectedNodeId, () => {
  loadInspectorCollections()
})

watch(currentSignature, (signature) => {
  if (hydrating || isViewMode.value) {
    return
  }
  if (signature === lastSavedSignature) {
    draftState.value = 'saved'
    return
  }
  draftState.value = 'dirty'
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  if (!workflowForm.name.trim()) {
    return
  }
  autoSaveTimer = setTimeout(() => {
    handleAutoSave()
  }, 1400)
})

function resolveNodeDefinition(type) {
  return NODE_LIBRARY[normalizeNodeType(type)] || NODE_LIBRARY.transform
}

function normalizeNodeType(type) {
  const raw = String(type || '').trim().toLowerCase()
  if (raw === 'http' || raw === 'http_api') return 'http_request'
  if (['web_crawl', 'login_crawl', 'pagination_crawl', 'crawl', 'spider'].includes(raw)) return 'web_crawl'
  if (['qq_email', 'qq_email_notice', 'email'].includes(raw)) return 'notification'
  if (['extract', 'structured_extract', 'field_transform', 'data_process'].includes(raw)) return 'transform'
  if (['task_result', 'task_result_read', 'ai_workflow', 'workflow'].includes(raw)) return 'ai_filter'
  return raw || 'transform'
}

function normalizeNodeConfig(type, rawConfig = {}) {
  const definition = resolveNodeDefinition(type)
  const config = safeClone(typeof rawConfig === 'string' ? safeJsonParse(rawConfig, {}) : rawConfig || {})
  const merged = {
    ...safeClone(definition.defaultConfig),
    ...config
  }

  if (type === 'web_crawl') {
    merged.headers = toPlainObject(merged.headers)
    merged.cookies = Array.isArray(merged.cookies) ? merged.cookies : []
    merged.extractionRules = Array.isArray(merged.extractionRules) ? merged.extractionRules : []
    merged.pagination = {
      maxPages: 1,
      nextSelector: '',
      pageParam: '',
      ...(merged.pagination || {})
    }
    merged.login = {
      loginUrl: '',
      submitSelector: '',
      usernameSelector: '',
      passwordSelector: '',
      ...(merged.login || {})
    }
  }

  if (type === 'http_request') {
    merged.headers = toPlainObject(merged.headers)
    merged.method = merged.method || 'GET'
    merged.timeout = Number(merged.timeout || 30000)
    merged.body = merged.body ?? ''
  }

  if (type === 'parallel_split') {
    merged.branches = Array.isArray(merged.branches) && merged.branches.length ? merged.branches : ['branch_a', 'branch_b']
  }

  if (type === 'transform') {
    merged.mappings = toPlainObject(merged.mappings)
  }

  if (type === 'condition') {
    merged.operator = merged.operator || 'equals'
    merged.leftPath = merged.leftPath || ''
    merged.rightValue = merged.rightValue ?? ''
    merged.rightPath = merged.rightPath || ''
  }

  if (type === 'ai_filter') {
    merged.temperature = Number(merged.temperature ?? 0.2)
    merged.outputFormat = merged.outputFormat || 'text'
  }

  if (type === 'notification') {
    merged.channel = merged.channel || 'email'
    merged.recipients = merged.recipients || merged.to || ''
    merged.subject = merged.subject || ''
    merged.bodyTemplate = merged.bodyTemplate || merged.body || ''
  }

  return merged
}

function getInputHandles(node) {
  const type = normalizeNodeType(node.type)
  if (type === 'start') {
    return []
  }
  return [{ id: 'in', label: 'in' }]
}

function getOutputHandles(node) {
  const type = normalizeNodeType(node.type)
  if (type === 'start') {
    return [{ id: 'out', label: 'out' }]
  }
  if (type === 'condition') {
    return [
      { id: 'true', label: 'true' },
      { id: 'false', label: 'false' },
      { id: 'error', label: 'error' }
    ]
  }
  if (type === 'parallel_split') {
    const branches = Array.isArray(node.config?.branches) && node.config.branches.length
      ? node.config.branches
      : ['branch_a', 'branch_b']
    return [
      ...branches.map((branch) => ({ id: String(branch), label: String(branch) })),
      { id: 'error', label: 'error' }
    ]
  }
  if (type === 'merge') {
    return [{ id: 'out', label: 'out' }]
  }
  if (type === 'end') {
    return []
  }
  return [
    { id: 'success', label: 'success' },
    { id: 'error', label: 'error' }
  ]
}

function defaultSourceHandle(type) {
  const normalized = normalizeNodeType(type)
  if (normalized === 'start' || normalized === 'merge') return 'out'
  if (normalized === 'condition') return 'true'
  if (normalized === 'parallel_split') return 'branch_a'
  return 'success'
}

function handlePositionStyle(index, total) {
  return {
    top: `${((index + 1) * 100) / (total + 1)}%`
  }
}

function safeJsonParse(value, fallback) {
  if (value === null || value === undefined || value === '') {
    return fallback
  }
  if (typeof value === 'object') {
    return value
  }
  try {
    return JSON.parse(value)
  } catch (error) {
    return fallback
  }
}

function safeClone(value) {
  return JSON.parse(JSON.stringify(value))
}

function toPlainObject(value) {
  if (!value || Array.isArray(value)) return {}
  if (typeof value === 'object') return value
  return safeJsonParse(value, {})
}

function createSchemaField() {
  return {
    id: createId('field'),
    name: '',
    title: '',
    type: 'string',
    description: '',
    required: false,
    format: '',
    enumText: '',
    defaultValue: ''
  }
}

function createId(prefix) {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`
}

function createDefaultGraph() {
  return {
    nodes: [
      {
        id: 'start_1',
        type: 'start',
        label: '开始',
        description: '',
        position: { x: 120, y: 200 },
        config: normalizeNodeConfig('start', {})
      },
      {
        id: 'end_1',
        type: 'end',
        label: '结束',
        description: '',
        position: { x: 480, y: 200 },
        config: normalizeNodeConfig('end', {})
      }
    ],
    edges: [
      {
        id: 'edge_start_1_end_1',
        source: 'start_1',
        sourceHandle: 'out',
        target: 'end_1',
        targetHandle: 'in'
      }
    ]
  }
}

function buildInputSchema() {
  const properties = {}
  const required = []

  inputFields.value.forEach((field) => {
    const name = String(field.name || '').trim()
    if (!name) return

    const schema = {
      type: field.type || 'string'
    }
    if (field.title) schema.title = field.title.trim()
    if (field.description) schema.description = field.description.trim()
    if (field.format) schema.format = field.format.trim()

    const enumValues = String(field.enumText || '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
    if (enumValues.length) {
      schema.enum = enumValues
    }

    if (String(field.defaultValue || '').trim()) {
      try {
        if (field.type === 'array' || field.type === 'object') {
          schema.default = JSON.parse(field.defaultValue)
        } else if (field.type === 'integer' || field.type === 'number') {
          schema.default = Number(field.defaultValue)
        } else if (field.type === 'boolean') {
          schema.default = String(field.defaultValue).trim() === 'true'
        } else {
          schema.default = field.defaultValue
        }
      } catch (error) {
        schema.default = field.defaultValue
      }
    }

    properties[name] = schema
    if (field.required) {
      required.push(name)
    }
  })

  return {
    type: 'object',
    properties,
    required
  }
}

function buildGraphPayload() {
  return {
    version: 2,
    nodes: graphNodes.value.map((node) => ({
      id: node.id,
      type: normalizeNodeType(node.type),
      label: node.label,
      description: node.description || '',
      position: {
        x: Math.round(node.position?.x || 0),
        y: Math.round(node.position?.y || 0)
      },
      config: sanitizeConfigForPayload(node)
    })),
    edges: graphEdges.value.map((edge) => ({
      id: edge.id,
      source: edge.source,
      sourceHandle: edge.sourceHandle || defaultSourceHandle(findNode(edge.source)?.type),
      target: edge.target,
      targetHandle: edge.targetHandle || 'in'
    }))
  }
}

function sanitizeConfigForPayload(node) {
  const type = normalizeNodeType(node.type)
  const config = safeClone(node.config || {})
  if (type === 'notification') {
    config.body = config.bodyTemplate
    config.to = config.recipients
  }
  return config
}

function buildPayload() {
  return {
    name: workflowForm.name.trim(),
    workflowCode: workflowForm.workflowCode.trim(),
    description: workflowForm.description,
    category: workflowForm.category,
    status: workflowForm.status,
    inputSchema: JSON.stringify(buildInputSchema(), null, 2),
    graph: JSON.stringify(buildGraphPayload(), null, 2)
  }
}

function parseSchemaFields(schemaValue) {
  const schema = safeJsonParse(schemaValue, { type: 'object', properties: {}, required: [] })
  const properties = schema?.properties || {}
  const required = schema?.required || []
  return Object.entries(properties).map(([name, config]) => ({
    id: createId('field'),
    name,
    title: config.title || '',
    type: config.type || 'string',
    description: config.description || '',
    required: required.includes(name),
    format: config.format || '',
    enumText: Array.isArray(config.enum) ? config.enum.join(',') : '',
    defaultValue: config.default === undefined ? '' : JSON.stringify(config.default)
  }))
}

function parseGraph(graphValue) {
  const rawGraph = safeJsonParse(graphValue, null)
  if (!rawGraph || !Array.isArray(rawGraph.nodes) || rawGraph.nodes.length === 0) {
    return createDefaultGraph()
  }

  const nodes = rawGraph.nodes.map((node, index) => {
    const type = normalizeNodeType(node.type)
    return {
      id: node.id || createId(type),
      type,
      label: node.label || resolveNodeDefinition(type).name,
      description: node.description || '',
      position: {
        x: Number(node.position?.x ?? 120 + index * 260),
        y: Number(node.position?.y ?? 180)
      },
      config: normalizeNodeConfig(type, node.config || {})
    }
  })

  const validNodeIds = new Set(nodes.map((node) => node.id))
  const edges = Array.isArray(rawGraph.edges)
    ? rawGraph.edges
      .filter((edge) => validNodeIds.has(edge.source) && validNodeIds.has(edge.target))
      .map((edge) => ({
        id: edge.id || createId('edge'),
        source: edge.source,
        sourceHandle: edge.sourceHandle || defaultSourceHandle(findNodeFromList(nodes, edge.source)?.type),
        target: edge.target,
        targetHandle: edge.targetHandle || 'in'
      }))
    : []

  return {
    nodes,
    edges: edges.length ? edges : createDefaultGraph().edges
  }
}

function findNode(id) {
  return graphNodes.value.find((node) => node.id === id) || null
}

function findNodeFromList(nodes, id) {
  return nodes.find((node) => node.id === id) || null
}

function loadInspectorCollections() {
  if (!selectedNode.value) {
    headerRows.value = []
    cookieRows.value = []
    extractionRuleRows.value = []
    mappingRows.value = []
    branchRows.value = []
    return
  }

  headerRows.value = objectToRows(selectedNode.value.config.headers)
  cookieRows.value = (selectedNode.value.config.cookies || []).map((item) => ({
    id: createId('cookie'),
    key: item.name || '',
    value: item.value || ''
  }))
  extractionRuleRows.value = (selectedNode.value.config.extractionRules || []).map((rule) => ({
    id: createId('rule'),
    field: rule.field || '',
    selector: rule.selector || '',
    type: rule.type || 'text',
    attr: rule.attr || ''
  }))
  mappingRows.value = objectToRows(selectedNode.value.config.mappings)
  branchRows.value = (selectedNode.value.config.branches || []).map((branch) => ({
    id: createId('branch'),
    value: String(branch)
  }))
}

function objectToRows(objectValue) {
  return Object.entries(toPlainObject(objectValue)).map(([key, value]) => ({
    id: createId('row'),
    key,
    value: String(value)
  }))
}

function rowsToObject(rows) {
  return rows.reduce((result, row) => {
    const key = String(row.key || '').trim()
    if (!key) return result
    result[key] = row.value
    return result
  }, {})
}

function addHeaderRow() {
  headerRows.value.push({ id: createId('header'), key: '', value: '' })
}

function removeHeaderRow(index) {
  headerRows.value.splice(index, 1)
  syncHeaderRows()
}

function syncHeaderRows() {
  if (!selectedNode.value) return
  selectedNode.value.config.headers = rowsToObject(headerRows.value)
}

function addCookieRow() {
  cookieRows.value.push({ id: createId('cookie'), key: '', value: '' })
}

function removeCookieRow(index) {
  cookieRows.value.splice(index, 1)
  syncCookieRows()
}

function syncCookieRows() {
  if (!selectedNode.value) return
  selectedNode.value.config.cookies = cookieRows.value
    .filter((row) => String(row.key || '').trim())
    .map((row) => ({ name: row.key.trim(), value: row.value || '' }))
}

function addExtractionRule() {
  extractionRuleRows.value.push({ id: createId('rule'), field: '', selector: '', type: 'text', attr: '' })
}

function removeExtractionRule(index) {
  extractionRuleRows.value.splice(index, 1)
  syncExtractionRules()
}

function syncExtractionRules() {
  if (!selectedNode.value) return
  selectedNode.value.config.extractionRules = extractionRuleRows.value
    .filter((row) => String(row.field || '').trim() || String(row.selector || '').trim())
    .map((row) => ({
      field: row.field || '',
      selector: row.selector || '',
      type: row.type || 'text',
      attr: row.attr || ''
    }))
}

function addMappingRow() {
  mappingRows.value.push({ id: createId('mapping'), key: '', value: '' })
}

function removeMappingRow(index) {
  mappingRows.value.splice(index, 1)
  syncMappingRows()
}

function syncMappingRows() {
  if (!selectedNode.value) return
  selectedNode.value.config.mappings = rowsToObject(mappingRows.value)
}

function addBranchRow() {
  branchRows.value.push({ id: createId('branch'), value: '' })
}

function removeBranchRow(index) {
  branchRows.value.splice(index, 1)
  syncBranchRows()
}

function syncBranchRows() {
  if (!selectedNode.value) return
  selectedNode.value.config.branches = branchRows.value
    .map((row) => String(row.value || '').trim())
    .filter(Boolean)
  sanitizeEdgesForNode(selectedNode.value.id)
}

function sanitizeEdgesForNode(nodeId) {
  const node = findNode(nodeId)
  if (!node) return
  const validHandles = new Set(getOutputHandles(node).map((handle) => handle.id))
  graphEdges.value = graphEdges.value.filter((edge) => {
    if (edge.source !== nodeId && edge.target !== nodeId) {
      return true
    }
    if (edge.source === nodeId) {
      if (normalizeNodeType(node.type) === 'end') return false
      return validHandles.has(edge.sourceHandle || defaultSourceHandle(node.type))
    }
    if (edge.target === nodeId && normalizeNodeType(node.type) === 'start') {
      return false
    }
    return true
  })
}

function removeSelectedNode() {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  graphNodes.value = graphNodes.value.filter((node) => node.id !== nodeId)
  graphEdges.value = graphEdges.value.filter((edge) => edge.source !== nodeId && edge.target !== nodeId)
  selectedNodeId.value = ''
}

function removeSelectedEdge() {
  if (!selectedEdge.value) return
  graphEdges.value = graphEdges.value.filter((edge) => edge.id !== selectedEdge.value.id)
  selectedEdgeId.value = ''
}

function addSchemaField() {
  inputFields.value.push(createSchemaField())
}

function removeSchemaField(index) {
  inputFields.value.splice(index, 1)
}

function ensureWorkflowBasics() {
  if (!workflowForm.name.trim()) {
    throw new Error('请先填写流程名称')
  }
  if (!workflowForm.workflowCode.trim()) {
    workflowForm.workflowCode = generateWorkflowCode(workflowForm.name)
  }
}

function generateWorkflowCode(name) {
  return String(name || 'workflow')
    .replace(/[^A-Za-z0-9\u4e00-\u9fa5]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .toUpperCase() || 'WORKFLOW'
}

async function handleAutoSave() {
  if (saving.value || publishing.value || drafting.value || debugging.value) return
  try {
    draftState.value = 'saving'
    await saveDraft({ silent: true })
  } catch (error) {
    draftState.value = 'error'
  }
}

async function saveDraft({ silent = false } = {}) {
  ensureWorkflowBasics()
  const payload = buildPayload()
  const response = workflowId.value
    ? await updateWorkflow(workflowId.value, payload)
    : await createWorkflow(payload)

  if (!workflowId.value && response.data?.id) {
    await router.replace({ path: '/workflow/design', query: { id: String(response.data.id), mode: 'edit' } })
  }

  workflowForm.status = response.data?.status || workflowForm.status
  lastSavedSignature = currentSignature.value
  draftState.value = 'saved'
  if (!silent) {
    ElMessage.success(response.message || '草稿已保存')
  }
  return response
}

async function handleSave() {
  if (isViewMode.value) return
  saving.value = true
  try {
    await saveDraft()
  } catch (error) {
    ElMessage.error(error.message || '保存草稿失败')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (isViewMode.value) return
  publishing.value = true
  try {
    if (validationIssues.value.length) {
      throw new Error(validationIssues.value[0])
    }
    await saveDraft({ silent: true })
    const response = await publishWorkflow(workflowId.value)
    workflowForm.status = response.data?.status || 'published'
    ElMessage.success(response.message || '流程已发布')
    await loadWorkflow()
  } catch (error) {
    ElMessage.error(error.message || '流程发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleGenerateDraft() {
  if (isViewMode.value) return
  if (!assistantPrompt.value.trim()) {
    ElMessage.warning('请先描述你想生成的流程')
    return
  }

  drafting.value = true
  try {
    const response = await createWorkflowDraft({
      prompt: assistantPrompt.value.trim(),
      currentGraph: JSON.stringify(buildGraphPayload(), null, 2),
      currentInputSchema: JSON.stringify(buildInputSchema(), null, 2)
    })
    const draft = response.data || {}
    workflowForm.name = draft.name || workflowForm.name
    workflowForm.description = draft.description || workflowForm.description
    workflowForm.category = draft.category || workflowForm.category
    workflowForm.status = 'draft'
    if (!workflowForm.workflowCode && workflowForm.name) {
      workflowForm.workflowCode = generateWorkflowCode(workflowForm.name)
    }
    inputFields.value = parseSchemaFields(draft.inputSchema)
    const parsedGraph = parseGraph(draft.graph)
    graphNodes.value = parsedGraph.nodes
    graphEdges.value = parsedGraph.edges
    assistantWarnings.value = Array.isArray(draft.warnings) ? draft.warnings : []
    selectedNodeId.value = ''
    selectedEdgeId.value = ''
    await handleAutoLayout()
    ElMessage.success('AI 初版流程图已生成，可以继续在画布中细化')
  } catch (error) {
    ElMessage.error(error.message || 'AI 生成流程图失败')
  } finally {
    drafting.value = false
  }
}

function createNode(type, position) {
  const definition = resolveNodeDefinition(type)
  return {
    id: `${type}_${Date.now()}_${Math.random().toString(36).slice(2, 5)}`,
    type,
    label: definition.name,
    description: '',
    position,
    config: normalizeNodeConfig(type, {})
  }
}

function handleAddNode() {
  if (isViewMode.value) return
  const type = insertNodeType.value || 'http_request'
  if (selectedEdge.value) {
    if (type === 'start' || type === 'end') {
      ElMessage.warning('开始/结束节点不能直接插入现有连线')
      return
    }
    insertNodeOnEdge(type, selectedEdge.value)
    return
  }

  const baseNode = selectedNode.value
  const position = baseNode
    ? { x: baseNode.position.x + 260, y: baseNode.position.y + 24 }
    : { x: 220 + graphNodes.value.length * 60, y: 180 + graphNodes.value.length * 30 }
  const node = createNode(type, position)
  graphNodes.value = [...graphNodes.value, node]

  if (baseNode && normalizeNodeType(baseNode.type) !== 'end') {
    const sourceHandle = getOutputHandles(baseNode)[0]?.id || defaultSourceHandle(baseNode.type)
    graphEdges.value = [
      ...graphEdges.value,
      {
        id: createId('edge'),
        source: baseNode.id,
        sourceHandle,
        target: node.id,
        targetHandle: 'in'
      }
    ]
  }

  selectedNodeId.value = node.id
  selectedEdgeId.value = ''
  nextTick(() => handleFitView())
}

function insertNodeOnEdge(type, edge) {
  const sourceNode = findNode(edge.source)
  const targetNode = findNode(edge.target)
  if (!sourceNode || !targetNode) return

  const node = createNode(type, {
    x: (sourceNode.position.x + targetNode.position.x) / 2,
    y: (sourceNode.position.y + targetNode.position.y) / 2
  })

  graphNodes.value = [...graphNodes.value, node]
  graphEdges.value = graphEdges.value
    .filter((item) => item.id !== edge.id)
    .concat([
      {
        id: createId('edge'),
        source: edge.source,
        sourceHandle: edge.sourceHandle || defaultSourceHandle(sourceNode.type),
        target: node.id,
        targetHandle: 'in'
      },
      {
        id: createId('edge'),
        source: node.id,
        sourceHandle: getOutputHandles(node)[0]?.id || defaultSourceHandle(node.type),
        target: edge.target,
        targetHandle: edge.targetHandle || 'in'
      }
    ])

  selectedEdgeId.value = ''
  selectedNodeId.value = node.id
  nextTick(() => handleFitView())
}

function handleConnect(connection) {
  if (isViewMode.value) return
  if (!validateConnection(connection)) {
    ElMessage.warning('这条连线不符合当前节点结构或会引入冲突')
    return
  }

  const sourceNode = findNode(connection.source)
  graphEdges.value = [
    ...graphEdges.value,
    {
      id: createId('edge'),
      source: connection.source,
      sourceHandle: connection.sourceHandle || defaultSourceHandle(sourceNode?.type),
      target: connection.target,
      targetHandle: connection.targetHandle || 'in'
    }
  ]
}

function validateConnection(connection) {
  const sourceNode = findNode(connection.source)
  const targetNode = findNode(connection.target)
  if (!sourceNode || !targetNode) return false
  if (connection.source === connection.target) return false
  if (normalizeNodeType(sourceNode.type) === 'end') return false
  if (normalizeNodeType(targetNode.type) === 'start') return false

  const sourceHandle = connection.sourceHandle || defaultSourceHandle(sourceNode.type)
  const targetHandle = connection.targetHandle || 'in'
  const validSourceHandles = getOutputHandles(sourceNode).map((handle) => handle.id)
  if (!validSourceHandles.includes(sourceHandle)) return false
  if (targetHandle !== 'in') return false

  if (normalizeNodeType(targetNode.type) !== 'merge') {
    const incomingCount = graphEdges.value.filter((edge) => edge.target === connection.target).length
    if (incomingCount >= 1) return false
  }

  if (normalizeNodeType(sourceNode.type) !== 'parallel_split') {
    const handleAlreadyUsed = graphEdges.value.some(
      (edge) => edge.source === connection.source && (edge.sourceHandle || defaultSourceHandle(sourceNode.type)) === sourceHandle
    )
    if (handleAlreadyUsed) return false
  }

  return !graphEdges.value.some(
    (edge) =>
      edge.source === connection.source &&
      edge.target === connection.target &&
      (edge.sourceHandle || defaultSourceHandle(sourceNode.type)) === sourceHandle &&
      (edge.targetHandle || 'in') === targetHandle
  )
}

function handleNodeClick(_event, node) {
  selectedNodeId.value = node.id
  selectedEdgeId.value = ''
}

function handleEdgeClick(_event, edge) {
  selectedEdgeId.value = edge.id
  selectedNodeId.value = ''
}

function clearSelection() {
  selectedNodeId.value = ''
  selectedEdgeId.value = ''
}

function handleNodeDragStop(_event, node) {
  const current = findNode(node.id)
  if (!current) return
  current.position = {
    x: Math.round(node.position.x),
    y: Math.round(node.position.y)
  }
}

function handleSelectedNodeTypeChange(type) {
  if (!selectedNode.value) return
  selectedNode.value.type = normalizeNodeType(type)
  selectedNode.value.label = resolveNodeDefinition(type).name
  selectedNode.value.config = normalizeNodeConfig(type, {})
  loadInspectorCollections()
  sanitizeEdgesForNode(selectedNode.value.id)
}

async function handleAutoLayout() {
  if (!graphNodes.value.length) return
  const dagreGraph = new dagre.graphlib.Graph()
  dagreGraph.setGraph({
    rankdir: 'LR',
    nodesep: 48,
    ranksep: 96,
    marginx: 24,
    marginy: 24
  })
  dagreGraph.setDefaultEdgeLabel(() => ({}))

  graphNodes.value.forEach((node) => {
    const size = getLayoutSize(node.type)
    dagreGraph.setNode(node.id, size)
  })
  graphEdges.value.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target)
  })

  dagre.layout(dagreGraph)
  graphNodes.value = graphNodes.value.map((node) => {
    const layoutNode = dagreGraph.node(node.id)
    const size = getLayoutSize(node.type)
    return layoutNode
      ? {
        ...node,
        position: {
          x: Math.round(layoutNode.x - size.width / 2),
          y: Math.round(layoutNode.y - size.height / 2)
        }
      }
      : node
  })
  await handleFitView()
}

function getLayoutSize(type) {
  const normalized = normalizeNodeType(type)
  if (normalized === 'start' || normalized === 'end') {
    return { width: 196, height: 98 }
  }
  if (normalized === 'parallel_split') {
    return { width: 244, height: 146 }
  }
  return { width: 238, height: 132 }
}

async function handleFitView() {
  await nextTick()
  try {
    fitView({ padding: 0.18, duration: 240 })
  } catch (error) {
    // ignore fit errors before the canvas is ready
  }
}

function validateGraph(nodes, edges) {
  const issues = []
  if (!nodes.length) {
    return ['流程图中至少需要一个节点']
  }

  const nodeMap = new Map(nodes.map((node) => [node.id, node]))
  const inDegree = {}
  const outDegree = {}
  const adjacency = {}
  const handleUsage = {}
  let startCount = 0
  let endCount = 0

  nodes.forEach((node) => {
    inDegree[node.id] = 0
    outDegree[node.id] = 0
    adjacency[node.id] = []
    if (normalizeNodeType(node.type) === 'start') startCount += 1
    if (normalizeNodeType(node.type) === 'end') endCount += 1
  })

  edges.forEach((edge) => {
    const sourceNode = nodeMap.get(edge.source)
    const targetNode = nodeMap.get(edge.target)
    if (!sourceNode || !targetNode) {
      issues.push(`连线 ${edge.id} 指向了不存在的节点`)
      return
    }

    const sourceHandle = edge.sourceHandle || defaultSourceHandle(sourceNode.type)
    const validSourceHandles = getOutputHandles(sourceNode).map((handle) => handle.id)
    if (!validSourceHandles.includes(sourceHandle)) {
      issues.push(`节点 ${sourceNode.label || sourceNode.id} 使用了非法输出 handle：${sourceHandle}`)
    }
    if ((edge.targetHandle || 'in') !== 'in') {
      issues.push(`节点 ${targetNode.label || targetNode.id} 只能使用 in 作为输入 handle`)
    }

    inDegree[edge.target] += 1
    outDegree[edge.source] += 1
    adjacency[edge.source].push(edge.target)

    const handleKey = `${edge.source}:${sourceHandle}`
    handleUsage[handleKey] = (handleUsage[handleKey] || 0) + 1
  })

  if (startCount !== 1) {
    issues.push('流程必须且只能有一个开始节点')
  }
  if (endCount < 1) {
    issues.push('流程至少需要一个结束节点')
  }
  if (!edges.length) {
    issues.push('流程图至少需要一条连线')
  }

  nodes.forEach((node) => {
    const type = normalizeNodeType(node.type)
    const incoming = inDegree[node.id]
    const outgoing = outDegree[node.id]
    if (type === 'start' && incoming > 0) {
      issues.push(`开始节点 ${node.label || node.id} 不能有入边`)
    }
    if (type === 'end' && outgoing > 0) {
      issues.push(`结束节点 ${node.label || node.id} 不能有出边`)
    }
    if (type !== 'start' && incoming === 0) {
      issues.push(`节点 ${node.label || node.id} 没有上游连接`)
    }
    if (type !== 'end' && outgoing === 0) {
      issues.push(`节点 ${node.label || node.id} 没有下游连接`)
    }
    if (type === 'merge' && incoming < 2) {
      issues.push(`汇聚节点 ${node.label || node.id} 至少需要两个上游分支`)
    }
    if (type !== 'merge' && incoming > 1) {
      issues.push(`只有汇聚节点允许多个入边：${node.label || node.id}`)
    }
    if (type === 'parallel_split') {
      const outgoingHandles = edges
        .filter((edge) => edge.source === node.id && edge.sourceHandle !== 'error')
        .map((edge) => edge.sourceHandle)
      if (new Set(outgoingHandles).size < 2) {
        issues.push(`并行分叉节点 ${node.label || node.id} 至少需要两个分支输出`)
      }
    }
    if (type !== 'parallel_split') {
      getOutputHandles(node).forEach((handle) => {
        const handleKey = `${node.id}:${handle.id}`
        if ((handleUsage[handleKey] || 0) > 1) {
          issues.push(`节点 ${node.label || node.id} 的 handle ${handle.id} 不能重复连向多个目标`)
        }
      })
    }
  })

  nodes.forEach((node) => {
    if (normalizeNodeType(node.type) !== 'condition') return
    const handles = new Set(edges.filter((edge) => edge.source === node.id).map((edge) => edge.sourceHandle))
    if (!handles.has('true') && !handles.has('false')) {
      issues.push(`条件节点 ${node.label || node.id} 至少需要 true 或 false 分支`)
    }
  })

  const degreeCopy = { ...inDegree }
  const queue = Object.keys(degreeCopy).filter((nodeId) => degreeCopy[nodeId] === 0)
  let visited = 0
  while (queue.length) {
    const nodeId = queue.shift()
    visited += 1
    adjacency[nodeId].forEach((target) => {
      degreeCopy[target] -= 1
      if (degreeCopy[target] === 0) {
        queue.push(target)
      }
    })
  }
  if (visited !== nodes.length) {
    issues.push('第一版只支持 DAG，当前流程图存在环路')
  }

  nodes.forEach((node) => {
    if (normalizeNodeType(node.type) !== 'parallel_split') return
    const splitEdges = edges.filter((edge) => edge.source === node.id && edge.sourceHandle !== 'error')
    if (splitEdges.length < 2) return

    let commonMerges = null
    splitEdges.forEach((edge) => {
      const merges = findReachableMergeNodes(edge.target, nodeMap, adjacency)
      if (commonMerges === null) {
        commonMerges = merges
      } else {
        commonMerges = new Set([...commonMerges].filter((id) => merges.has(id)))
      }
    })
    if (!commonMerges || commonMerges.size === 0) {
      issues.push(`并行分叉 ${node.label || node.id} 没有闭合到同一个汇聚节点`)
    }
  })

  return [...new Set(issues)]
}

function findReachableMergeNodes(startNodeId, nodeMap, adjacency) {
  const visited = new Set()
  const merges = new Set()
  const queue = [startNodeId]
  while (queue.length) {
    const nodeId = queue.shift()
    if (visited.has(nodeId)) continue
    visited.add(nodeId)
    const node = nodeMap.get(nodeId)
    if (!node) continue
    if (normalizeNodeType(node.type) === 'merge') {
      merges.add(nodeId)
      continue
    }
    adjacency[nodeId]?.forEach((next) => {
      if (!visited.has(next)) {
        queue.push(next)
      }
    })
  }
  return merges
}

async function loadNodeTypes() {
  try {
    const response = await getAllNodeTypes()
    nodeTypeOptions.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    nodeTypeOptions.value = []
  }
}

async function loadWorkflow() {
  hydrating = true
  try {
    if (!workflowId.value) {
      resetDesigner()
      if (mode.value === 'ai') {
        assistantPrompt.value = ''
      }
      return
    }

    const response = await getWorkflowById(workflowId.value)
    const workflow = response.data || {}
    workflowForm.name = workflow.name || ''
    workflowForm.workflowCode = workflow.workflowCode || ''
    workflowForm.description = workflow.description || ''
    workflowForm.category = workflow.category || 'data_collection'
    workflowForm.status = workflow.status || 'draft'
    inputFields.value = parseSchemaFields(workflow.inputSchema)
    const parsedGraph = parseGraph(workflow.graph || workflow.config)
    graphNodes.value = parsedGraph.nodes
    graphEdges.value = parsedGraph.edges
    assistantWarnings.value = []
    await loadDebugRuns()
  } finally {
    selectedNodeId.value = ''
    selectedEdgeId.value = ''
    loadInspectorCollections()
    await nextTick()
    lastSavedSignature = currentSignature.value
    draftState.value = 'saved'
    hydrating = false
    handleFitView()
  }
}

function resetDesigner() {
  workflowForm.name = ''
  workflowForm.workflowCode = ''
  workflowForm.description = ''
  workflowForm.category = 'data_collection'
  workflowForm.status = 'draft'
  inputFields.value = []
  const defaults = createDefaultGraph()
  graphNodes.value = defaults.nodes
  graphEdges.value = defaults.edges
  assistantWarnings.value = []
  debugRuns.value = []
  latestDebugRun.value = null
}

async function loadDebugRuns() {
  if (!workflowId.value) {
    debugRuns.value = []
    latestDebugRun.value = null
    return
  }
  try {
    const response = await getWorkflowDebugRuns(workflowId.value, { silent: true })
    debugRuns.value = Array.isArray(response.data) ? response.data : []
    if (debugRuns.value.length > 0) {
      await loadDebugRunDetail(debugRuns.value[0].runId, true)
    } else {
      latestDebugRun.value = null
    }
  } catch (error) {
    debugRuns.value = []
    latestDebugRun.value = null
  }
}

async function loadDebugRunDetail(runId, restartPolling = true) {
  if (!runId) return
  try {
    const response = await getWorkflowDebugRunDetail(runId, { silent: true })
    latestDebugRun.value = response.data
    if (restartPolling && ['pending', 'running'].includes(response.data?.status)) {
      startDebugPolling(runId)
    }
  } catch (error) {
    // noop
  }
}

function startDebugPolling(runId) {
  stopDebugPolling()
  debugPollingTimer = setInterval(async () => {
    await loadDebugRunDetail(runId, false)
    if (!latestDebugRun.value || !['pending', 'running'].includes(latestDebugRun.value.status)) {
      stopDebugPolling()
      await loadDebugRuns()
    }
  }, 2500)
}

function stopDebugPolling() {
  if (debugPollingTimer) {
    clearInterval(debugPollingTimer)
    debugPollingTimer = null
  }
}

async function handleDebugRun() {
  if (isViewMode.value) return
  debugging.value = true
  try {
    if (validationIssues.value.length) {
      throw new Error(validationIssues.value[0])
    }

    const debugInput = safeJsonParse(debugInputText.value, null)
    if (debugInput === null || Array.isArray(debugInput) || typeof debugInput !== 'object') {
      throw new Error('调试输入必须是合法 JSON 对象')
    }

    await saveDraft({ silent: true })
    const response = await createWorkflowDebugRun(workflowId.value, { inputConfig: debugInput })
    ElMessage.success(response.message || '草稿试跑已启动')
    await loadDebugRuns()
    if (response.data?.runId) {
      await loadDebugRunDetail(response.data.runId)
      startDebugPolling(response.data.runId)
    }
  } catch (error) {
    ElMessage.error(error.message || '草稿试跑失败')
  } finally {
    debugging.value = false
  }
}

function debugStatusType(status) {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'running') return 'warning'
  return 'info'
}

function handleBack() {
  router.push('/workflow/list')
}

onMounted(async () => {
  await loadNodeTypes()
  await loadWorkflow()
})

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  stopDebugPolling()
})
</script>

<style lang="scss">
@import '@vue-flow/core/dist/style.css';
@import '@vue-flow/core/dist/theme-default.css';
</style>

<style scoped lang="scss">
.workflow-designer-page {
  --designer-bg: radial-gradient(circle at top left, rgba(255, 122, 24, 0.12), transparent 32%),
    radial-gradient(circle at top right, rgba(21, 164, 250, 0.12), transparent 28%),
    linear-gradient(180deg, #f7f7f2 0%, #f1f5f9 100%);
  min-height: 100%;
  padding: 6px;
  background: var(--designer-bg);
}

.designer-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.designer-header,
.ai-strip,
.canvas-stage,
.inspector-panel,
.info-panel {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.designer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 26px;
}

.designer-title-group h2,
.ai-strip-copy h3,
.panel-header h3 {
  margin: 0;
  font-family: 'Trebuchet MS', 'Segoe UI', 'PingFang SC', sans-serif;
  letter-spacing: 0.01em;
}

.designer-title-group p,
.ai-strip-copy p,
.section-inline-head p,
.toolbar-hint,
.node-card-desc,
.panel-kicker,
.node-card-foot,
.node-id,
.debug-summary,
.step-meta {
  color: #64748b;
}

.eyebrow,
.ai-badge,
.panel-kicker {
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  font-weight: 700;
}

.eyebrow {
  margin-bottom: 8px;
  color: #0f766e;
}

.designer-title-group p {
  margin: 8px 0 0;
  max-width: 720px;
  line-height: 1.6;
}

.designer-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.draft-indicator {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  font-size: 12px;
  font-weight: 600;
}

.draft-indicator[data-state='saved'] {
  color: #0f766e;
  background: rgba(15, 118, 110, 0.12);
}

.draft-indicator[data-state='dirty'] {
  color: #b45309;
  background: rgba(245, 158, 11, 0.14);
}

.draft-indicator[data-state='saving'] {
  color: #1d4ed8;
  background: rgba(37, 99, 235, 0.14);
}

.draft-indicator[data-state='error'] {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.14);
}

.ai-strip {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 18px;
  padding: 22px 24px;
}

.ai-strip-copy {
  padding-right: 8px;
}

.ai-badge {
  margin-bottom: 10px;
  color: #ff7a18;
}

.ai-strip-copy p {
  margin: 8px 0 0;
  line-height: 1.6;
}

.ai-strip-input {
  display: flex;
  gap: 12px;
  align-items: center;
}

.ai-strip-input :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 18px;
}

.designer-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 18px;
}

.canvas-stage {
  padding: 18px;
  min-height: 760px;
}

.stage-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
}

.toolbar-label {
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  letter-spacing: 0.04em;
}

.toolbar-hint {
  font-size: 13px;
  line-height: 1.5;
}

.stage-canvas {
  height: 680px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background:
    linear-gradient(135deg, rgba(255, 122, 24, 0.06), transparent 40%),
    linear-gradient(225deg, rgba(37, 99, 235, 0.06), transparent 38%),
    #fcfcfa;
}

.workflow-flow {
  height: 100%;
}

.designer-node {
  position: relative;
  min-width: 214px;
  min-height: 118px;
  padding: 14px 16px;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.92));
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.designer-node::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 6px;
  border-radius: 20px 0 0 20px;
  background: var(--node-accent);
}

.designer-node.is-selected {
  border-color: rgba(255, 122, 24, 0.55);
  box-shadow: 0 0 0 4px rgba(255, 122, 24, 0.16), 0 20px 40px rgba(255, 122, 24, 0.16);
}

.node-card-head,
.node-card-foot,
.step-head,
.schema-card-head,
.section-inline-head,
.editor-head,
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.node-kind {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  font-size: 11px;
  font-weight: 700;
}

.node-id {
  font-size: 11px;
}

.node-card-title {
  margin-top: 12px;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.node-card-desc {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.55;
}

.node-card-foot {
  margin-top: 12px;
  font-size: 11px;
  font-weight: 600;
}

.handle-wrap {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  transform: translateY(-50%);
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}

.handle-wrap-left {
  left: -8px;
}

.handle-wrap-right {
  right: -8px;
}

.flow-handle {
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  background: var(--node-accent);
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.15);
}

.inspector-stage {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 760px;
}

.inspector-panel,
.info-panel {
  padding: 18px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.panel-header.compact {
  margin-bottom: 12px;
}

.panel-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 12px;
}

.panel-body {
  font-size: 14px;
}

.scrollable {
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.compact-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.two-col-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.collection-editor,
.schema-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.editor-head {
  margin-top: 4px;
}

.row-editor {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 10px;
  align-items: center;
}

.rule-editor {
  display: grid;
  grid-template-columns: 1fr 1.2fr 110px 110px auto;
  gap: 10px;
  align-items: center;
}

.schema-card,
.step-card,
.empty-state-card,
.debug-summary {
  padding: 14px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.empty-inline {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.04);
  color: #64748b;
  font-size: 12px;
}

.validation-ok {
  padding: 12px 14px;
  border-radius: 16px;
  color: #0f766e;
  background: rgba(15, 118, 110, 0.1);
  font-weight: 600;
}

.issue-list,
.warning-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 18px;
  margin: 0;
}

.issue-list li {
  color: #b45309;
  line-height: 1.5;
}

.warning-list li {
  color: #7c2d12;
  line-height: 1.5;
}

.debug-run-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 14px 0 12px;
}

.debug-run-chip {
  border: none;
  border-radius: 999px;
  padding: 8px 12px;
  background: rgba(15, 23, 42, 0.06);
  color: #334155;
  font-size: 12px;
  cursor: pointer;
}

.debug-run-chip.active {
  background: rgba(255, 122, 24, 0.15);
  color: #c2410c;
}

.summary-row {
  padding: 6px 0;
  font-size: 13px;
}

.debug-error,
.step-error {
  margin-top: 10px;
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.5;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.step-meta {
  margin-top: 6px;
  font-size: 12px;
}

@media (max-width: 1500px) {
  .designer-content {
    grid-template-columns: minmax(0, 1fr) 340px;
  }
}

@media (max-width: 1280px) {
  .ai-strip,
  .designer-content {
    grid-template-columns: 1fr;
  }

  .stage-canvas {
    height: 620px;
  }
}

@media (max-width: 960px) {
  .designer-header,
  .ai-strip-input,
  .stage-toolbar {
    flex-direction: column;
  }

  .two-col-grid,
  .row-editor,
  .rule-editor {
    grid-template-columns: 1fr;
  }

  .stage-canvas {
    height: 560px;
  }
}
</style>
