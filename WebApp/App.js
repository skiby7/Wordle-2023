import './App.css';
import Ranking from './components/Ranking';
import Sidebar from './components/Sidebar';
import Home from './components/Home';
import { BrowserRouter as Router, Routes, Route, Navigate} from 'react-router-dom';
// import { Switch, Route, Redirect } from 'react-router-dom'
import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import { Toaster } from 'react-hot-toast';
import { host } from './components/Global'
import axios from 'axios';
function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [user, setUser] = useState({
    username: '',
    token: '',
    wordId: -1
  });
  const [isDarkMode, setIsDarkMode] = useState(false);
  const setAuth = (boolean) => {
    setIsAuthenticated(boolean)
    if (!boolean) {
      localStorage.removeItem('username');
      localStorage.removeItem('token');
      setIsPlaying(false);
    }
  }
  async function isAuth() {
    try {
      const savedUsername = localStorage.getItem('username');
      const oldToken = localStorage.getItem('token')
      if (savedUsername === null || oldToken === null){
        return false;
      }
      const response = await fetch(`${host}/verify?username=${savedUsername}&token=${oldToken}`, {
        method: "GET",
        withCredentials: true,
        credentials: 'include',
        headers : new Headers({
          "Authorization" : oldToken
        })
      })

      const parseRes = await response.json()
      parseRes.token === 'True' ? setIsAuthenticated(true) : setIsAuthenticated(false)
      if (parseRes.token === 'True'){
        setUser({username: savedUsername, token: oldToken})
      	axios.defaults.headers.common['Authorization'] = 'Bearer ' + user.token

      } 
    } catch (error) {
      console.error(error.message)
    }
  }

  
  useEffect(() => {
    isAuth();
  }, [])

  return (
    <Router>
      <div className='flex flex-row justify-center'>
        <Toaster className='flex top-20'/>
        <Sidebar user={user} setAuth={setAuth} setIsDarkMode={setIsDarkMode}/>
        {/* <Login setAuth={setAuth}></Login> */}
        <Routes>
          <Route exact path='/' element={!isAuthenticated ? (<Login setAuth={setAuth} setUsername={setUser} />) : (<Home user={user} setUser={setUser} isPlaying={isPlaying} setIsPlaying={setIsPlaying} setAuth={setAuth} darkMode={isDarkMode}/>)} />
          <Route exact path='/ranking' element={!isAuthenticated ? (<Login setAuth={setAuth} setusername={setUser}/>) : (<Ranking user={user} setAuth={setAuth} />)} />
          <Route exact path='/login' element={!isAuthenticated ? (<Login setAuth={setAuth} setusername={setUser}/>) : (<Navigate to='/' />)} />
          {/* <Route exact path='/insert' element={!isAuthenticated ? (<Login setAuth={setAuth} setusername={setUser}/>) : (<InsertModal/>)} /> */}
        </Routes>
      </div>
    
     </Router>
    
    
  );
}

export default App;
