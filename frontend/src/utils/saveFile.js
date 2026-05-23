/**
 * 使用系统“另存为”对话框保存文件（支持时），否则回退为浏览器下载。
 * @returns {Promise<string>} 实际保存的文件名
 */
export async function saveBlobAs(blob, suggestedName) {
  const name = suggestedName.endsWith('.zip') ? suggestedName : `${suggestedName}.zip`

  if (typeof window.showSaveFilePicker === 'function') {
    const handle = await window.showSaveFilePicker({
      suggestedName: name,
      types: [
        {
          description: 'ZIP 压缩包',
          accept: { 'application/zip': ['.zip'] }
        }
      ]
    })
    const writable = await handle.createWritable()
    await writable.write(blob)
    await writable.close()
    return handle.name || name
  }

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
  return name
}
