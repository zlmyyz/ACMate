<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { basicSetup } from 'codemirror'
import { Compartment, EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'
import { indentWithTab } from '@codemirror/commands'
import { indentUnit, StreamLanguage } from '@codemirror/language'
import { autocompletion, closeCompletion, completeAnyWord, completeFromList, type CompletionContext } from '@codemirror/autocomplete'
import { cpp } from '@codemirror/lang-cpp'
import { java } from '@codemirror/lang-java'
import { python } from '@codemirror/lang-python'
import { javascript } from '@codemirror/lang-javascript'
import { go } from '@codemirror/lang-go'
import { rust } from '@codemirror/lang-rust'
import { csharp } from '@codemirror/legacy-modes/mode/clike'
import { practiceLanguages, practiceKeywords, type PracticeLanguage } from '@/constants/practice-languages'
import { oneDark } from '@codemirror/theme-one-dark'
const props = withDefaults(defineProps<{ modelValue: string; language: string; disabled?: boolean; autocomplete?: boolean }>(),{autocomplete:true})
const emit = defineEmits<{ 'update:modelValue': [value: string]; 'update:autocomplete':[value:boolean]; run: []; save: [] }>()
const host = ref<HTMLElement>(), expanded = ref(false), line = ref(1), column = ref(1)
const filename = computed(() => practiceLanguages[props.language as PracticeLanguage]?.filename ?? 'Main.txt')
const languageConfig = new Compartment(), readOnly = new Compartment(), completionConfig=new Compartment()
let view: EditorView | undefined
const extension = () => {
  switch(props.language){
    case 'JAVA17':return java()
    case 'PYTHON3':return python()
    case 'CSHARP':return StreamLanguage.define(csharp)
    case 'TYPESCRIPT':return javascript({typescript:true})
    case 'JAVASCRIPT':return javascript()
    case 'GO':return go()
    case 'RUST':return rust()
    default:return cpp()
  }
}
const completionExtension=()=>autocompletion({activateOnTyping:props.autocomplete,override:props.autocomplete?null:[]})
const keywordCompletion=(context:CompletionContext)=>completeFromList((practiceKeywords[props.language as PracticeLanguage]??[]).map(label=>({label,type:'keyword'})))(context)
function toggleSize() { expanded.value = !expanded.value; view?.requestMeasure() }
onMounted(() => {
  view = new EditorView({ parent: host.value, state: EditorState.create({ doc: props.modelValue, extensions: [
    keymap.of([indentWithTab, { key: 'Mod-s', run: () => { emit('save'); return true } }, { key: "Mod-'", run: () => { if (!props.disabled) emit('run'); return true } }, { key: 'Mod-Enter', run: () => { if (!props.disabled) emit('run'); return true } }, { key: 'F11', run: () => { toggleSize(); return true } }, { key: 'Escape', run: () => { if (!expanded.value) return false; toggleSize(); return true } }]),
    basicSetup, oneDark, languageConfig.of(extension()),completionConfig.of(completionExtension()),EditorState.languageData.of(()=>[{autocomplete:keywordCompletion},{autocomplete:completeAnyWord}]),
    readOnly.of([EditorState.readOnly.of(!!props.disabled), EditorView.editable.of(!props.disabled)]),
    EditorState.tabSize.of(4), indentUnit.of('    '),
    EditorState.changeFilter.of(transaction => transaction.newDoc.length <= 65536),
    EditorView.contentAttributes.of({ 'aria-label': '代码编辑器', 'aria-multiline': 'true' }),
    EditorView.updateListener.of(update => {
      if (update.docChanged) emit('update:modelValue', update.state.doc.toString())
      const cursor = update.state.selection.main.head, current = update.state.doc.lineAt(cursor)
      line.value = current.number; column.value = cursor - current.from + 1
    }),
    EditorView.theme({
      '&': { height: '100%', backgroundColor: '#080a0d', color: '#e6edf3', fontSize: '14px' },
      '.cm-scroller': { fontFamily: "'Cascadia Code', Consolas, monospace", lineHeight: '1.75', overflow: 'auto' },
      '.cm-content': { padding: '14px 0', caretColor: '#f8fafc' },
      '.cm-gutters': { backgroundColor: '#080a0d', color: '#616b7b', border: 'none', paddingRight: '8px' },
      '.cm-activeLine, .cm-activeLineGutter': { backgroundColor: '#151a22' },
      '&.cm-focused .cm-selectionBackground, .cm-selectionBackground': { backgroundColor: '#26466b !important' },
      '&.cm-focused': { outline: 'none' }, '.cm-cursor': { borderLeftColor: '#f8fafc' },
    }, { dark: true }),
  ] }) })
})
watch(() => props.modelValue, value => { if (view && value !== view.state.doc.toString()) view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: value } }) })
watch(() => props.language, () => view?.dispatch({ effects: languageConfig.reconfigure(extension()) }))
watch(() => props.disabled, value => view?.dispatch({ effects: readOnly.reconfigure([EditorState.readOnly.of(!!value), EditorView.editable.of(!value)]) }))
watch(()=>props.autocomplete,()=>{if(view){if(!props.autocomplete)closeCompletion(view);view.dispatch({effects:completionConfig.reconfigure(completionExtension())})}})
onUnmounted(() => view?.destroy())
defineExpose({ getView: () => view })
</script>
<template>
<div class="code-editor-shell" :class="{ expanded }">
  <div class="code-toolbar"><span class="file-tab"><span class="file-dot" />{{ filename }}</span><div class="editor-tools"><label class="completion-toggle"><input type="checkbox" role="switch" aria-label="代码补全" :checked="autocomplete" :disabled="disabled" @change="emit('update:autocomplete',($event.target as HTMLInputElement).checked)"/>代码补全</label><button type="button" :aria-label="expanded ? '退出放大编辑器' : '放大编辑器'" @click="toggleSize">{{ expanded ? '还原' : '放大' }} <span>F11</span></button></div></div>
  <div ref="host" class="editor-host" />
  <div v-if="$slots.footer" class="editor-footer"><slot name="footer" /></div>
  <div class="code-status"><span>行 {{ line }}，列 {{ column }} · UTF-8 · Tab 4</span><span>Ctrl+' 自测 · Ctrl+S 保存</span></div>
  <details class="shortcut-help"><summary>编辑器快捷键</summary><div>Ctrl+' / Ctrl+Enter：自测运行；Tab / Shift+Tab：缩进 / 取消缩进；Ctrl+/：注释；Ctrl+Z / Ctrl+Shift+Z：撤销 / 重做；Ctrl+F：查找；Ctrl+H：替换；Ctrl+D：选中下一个相同词；Alt+↑ / ↓：移动行；F11：放大；Esc：退出放大。macOS 使用 ⌘ 代替 Ctrl。</div></details>
</div>
</template>
<style scoped>
.code-editor-shell{display:flex;flex-direction:column;overflow:hidden;border:1px solid #303743;border-radius:9px;background:#080a0d;color:#cbd5e1;min-width:0}.code-toolbar{display:flex;justify-content:space-between;align-items:center;background:#10141b;min-height:42px;padding:0 12px;border-bottom:1px solid #252c37;font:12px/1.5 var(--font-mono),monospace}.file-tab{display:flex;align-items:center;gap:8px}.file-dot{width:7px;height:7px;border-radius:50%;background:#5b9dff}.code-toolbar button{padding:4px 8px;border:0;border-radius:4px;background:transparent;color:#cad4e3;cursor:pointer;font:inherit}.code-toolbar button:hover{background:#252c37}.code-toolbar button span{color:#8390a4;margin-left:8px}.editor-host{height:clamp(520px,68dvh,900px);min-height:420px}.code-status{display:flex;justify-content:space-between;gap:10px;padding:7px 12px;font:11px/1.5 var(--font-mono),monospace;border-top:1px solid #252c37;color:#8390a4;flex-wrap:wrap}.shortcut-help{padding:6px 12px;background:#10141b;font-size:11px;color:#9aa8bc}.shortcut-help summary{cursor:pointer}.shortcut-help div{padding:8px 0;line-height:1.9}.expanded{position:fixed;inset:16px;z-index:2500;box-shadow:0 0 0 100vmax #000b}.editor-footer{min-width:0;flex-shrink:0}.expanded .editor-host{height:auto;min-height:120px;flex:1}.expanded .editor-footer{max-height:42dvh;overflow:auto}@media(max-width:650px){.editor-host{height:520px}.code-status span:last-child{display:none}.expanded{inset:6px}}
</style>
<style scoped>
.editor-tools{display:flex;gap:14px;align-items:center}.completion-toggle{display:inline-flex;gap:6px;align-items:center;cursor:pointer;user-select:none}.completion-toggle input{accent-color:#5b9dff}.completion-toggle:focus-within{outline:2px solid #5b9dff;outline-offset:3px}
</style>

