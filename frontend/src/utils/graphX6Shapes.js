import { Graph } from '@antv/x6'

let ready = false

function hiddenPort(position) {
  return {
    position,
    attrs: {
      circle: {
        r: 0,
        magnet: true,
        stroke: 'none',
        fill: 'transparent',
        visibility: 'hidden'
      }
    }
  }
}

const HIDDEN_PORTS = {
  groups: {
    top: hiddenPort('top'),
    bottom: hiddenPort('bottom'),
    left: hiddenPort('left'),
    right: hiddenPort('right')
  },
  items: [
    { id: 'top', group: 'top' },
    { id: 'bottom', group: 'bottom' },
    { id: 'left', group: 'left' },
    { id: 'right', group: 'right' }
  ]
}

const HIDDEN_PORTS_V = {
  groups: {
    top: hiddenPort('top'),
    bottom: hiddenPort('bottom')
  },
  items: [
    { id: 'top', group: 'top' },
    { id: 'bottom', group: 'bottom' }
  ]
}

function safeRegister(name, config) {
  try {
    Graph.registerNode(name, config)
  } catch {
    // 路由切换后组件重新挂载时，全局节点类型可能已注册
  }
}

export function ensureGraphShapes() {
  if (ready) return
  ready = true

  safeRegister('prop-box', {
    inherit: 'rect',
    attrs: {
      body: {
        fill: '#fff',
        stroke: '#111',
        strokeWidth: 1.2,
        rx: 0,
        ry: 0
      },
      label: {
        fill: '#111',
        fontSize: 13,
        fontWeight: 600,
        fontFamily: 'Inter, "Microsoft YaHei", sans-serif'
      }
    },
    ports: HIDDEN_PORTS
  })

  safeRegister('hub-support', {
    inherit: 'circle',
    width: 8,
    height: 8,
    attrs: { body: { fill: '#111', stroke: '#111' } },
    ports: HIDDEN_PORTS_V
  })

  safeRegister('hub-attack', {
    inherit: 'circle',
    width: 14,
    height: 14,
    attrs: { body: { fill: '#fff', stroke: '#111', strokeWidth: 1.8 } },
    ports: HIDDEN_PORTS_V
  })

  safeRegister('hub-plus', {
    inherit: 'circle',
    width: 22,
    height: 22,
    markup: [
      { tagName: 'circle', selector: 'body' },
      { tagName: 'text', selector: 'plus' }
    ],
    attrs: {
      body: {
        refCx: '50%',
        refCy: '50%',
        refR: '50%',
        fill: '#fff',
        stroke: '#111',
        strokeWidth: 1.8
      },
      plus: {
        text: '+',
        refX: '50%',
        refY: '50%',
        textAnchor: 'middle',
        textVerticalAnchor: 'middle',
        fontSize: 14,
        fontWeight: 700,
        fill: '#111'
      }
    },
    ports: HIDDEN_PORTS
  })
}
