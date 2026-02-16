import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import { ToastContainer } from './ui'
import useToast from './hooks/useToast'

function App() {
  const [count, setCount] = useState(0)
  const toast = useToast()

  const handleClick = () => {
    setCount((count) => count + 1)
    
    // Ejemplo de uso de toasts
    if (count % 5 === 0) {
      toast.success('¡Llegaste a un múltiplo de 5!')
    } else if (count % 3 === 0) {
      toast.warning('Cuidado, múltiplo de 3')
    } else {
      toast.info(`Contador: ${count + 1}`)
    }
  }

  return (
    <>
      <ToastContainer />
      
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div className="card">
        <button onClick={handleClick}>
          count is {count}
        </button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
      </div>
      <p className="read-the-docs">
        Click on the Vite and React logos to learn more
      </p>
    </>
  )
}

export default App
