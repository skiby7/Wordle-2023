import React, { useState } from 'react'
// import { Link } from 'react-router-dom'
import toast, { Toaster } from 'react-hot-toast';
import { BsEraser } from 'react-icons/bs';
import { host } from './Global'
import axios from 'axios';

function Login({ setAuth, setUsername }) {
    const [inputs, setInputs] = useState({
        usernameInput: "",
        passwordInput: "",
        confirmPasswordInput: "",
    })
    const [subscribe, setSubscribe] = useState(false);

    const { usernameInput, passwordInput, confirmPasswordInput } = inputs;
   
    const onChange = (e) => {
        setInputs({ ...inputs, [e.target.name]: e.target.value })
    }

    const onSubmit = async (e) => {
        e.preventDefault()
        try {

            const body = { usernameInput, passwordInput }
            if(subscribe){
                if(usernameInput == undefined || usernameInput.trim() === "") {
                    toast.error('Inserisci un nome utente!');
                    setInputs({usernameInput: "", passwordInput : "", confirmPasswordInput: ""});
                    return;
                }
                if(passwordInput == undefined || passwordInput.trim() === "") {
                    toast.error('Inserisci una password!');
                    setInputs({usernameInput: "", passwordInput : "", confirmPasswordInput: ""});
                    return;
                }
                if (confirmPasswordInput == undefined || confirmPasswordInput !== passwordInput) {
                    toast.error('Le passowrd non corrispondono!');
                    setInputs({usernameInput: "", passwordInput : "", confirmPasswordInput: ""});
                    return;
                }
                const response = await fetch(`${host}/register`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                    },
                    body: `username=${body.usernameInput}&password=${body.passwordInput}`
                })
    
                switch(response.status){
                    case 200:
                        toast.success('Utente registrato con successo!');
                        setSubscribe(false);
                        setInputs({usernameInput : "", passwordInput: ""});
                        break;
                    case 409:
                        toast.error('Nome utente già in uso, scegli un altro username!');
                        setSubscribe(true);
                        break;
                    case 500:
                        toast.error('Errore interno!');
                        setSubscribe(true);
                        break
                }
            }
            else {
                const response = await fetch(`${host}/login`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                    },
                    body: `username=${body.usernameInput}&password=${body.passwordInput}`
                })
    
                const parseRes = await response.json()
    
                if (parseRes.token) {
                    localStorage.setItem("token", parseRes.token)
                    localStorage.setItem("username", usernameInput)
                    setAuth(true)
                  	axios.defaults.headers.common['Authorization'] = 'Bearer ' + parseRes.token
                    setUsername({username: usernameInput, token: parseRes.token})
                    toast.success(`Autenticato come ${usernameInput}`)
                }
                else {
                    setAuth(false)
                    setUsername({username: '', token: ''})
                    toast.error(`Login non valido`)
                }
            }
            
        } catch (error) {
            console.error(error.message);
        }
    }

    return (
        <>
            <div className="flex flex-col w-screen h-screen">
                <div className='h-screen flex flex-row items-center justify-center bg-gray-bg1'>
                    <div className='flex flex-col items-center justify-between w-full max-w-md m-auto bg-gray-100 dark:bg-gray-800 rounded-lg shadow-2xl py-10 px-16'>
                            {subscribe ? <h1 className='text-2xl font-medium text-primary mt-4 mb-8 text-center text-black dark:text-white cursor-default'>
                         Registrati a Wordle </h1> : <h1 className='text-2xl font-medium text-primary mt-4 mb-8 text-center text-black dark:text-white cursor-default'>
                          Login Wordle</h1>}
                        
                        <form className="flex flex-col gap-3 justify-evenly items-center max-w-full w-full" onSubmit={onSubmit}>
                            <div>
                                <input placeholder='Username' type="text" className={`w-96 p-2 bg-gray-200 text-primary rounded-md shadow-md outline-none text-sm transition duration-150 ease-in-out mb-4`} name="usernameInput" value={usernameInput} onChange={e => onChange(e)} />
                            </div>
                            <div>
                                <input placeholder='Password' type="password" className={`w-96 p-2 bg-gray-200 text-primary rounded-md shadow-md outline-none text-sm transition duration-150 ease-in-out mb-4`} name="passwordInput" value={passwordInput} onChange={e => onChange(e)} />
                            </div>
                            {subscribe ? <div>
                                <input placeholder='Conferma password' type="password" className={`w-96 p-2 bg-gray-200 text-primary rounded-md md shadow-md outline-none text-sm transition duration-150 ease-in-out mb-4`} name="confirmPasswordInput" value={confirmPasswordInput} onChange={e => onChange(e)} />
                            </div> : <span></span>}
                            {subscribe ? <span className='dark:text-white text-gray-500'>Sei già registrato? Clicca <a href="#" className='underline' onClick={() => {setSubscribe(false)}}>qui</a>!</span> : <span className='dark:text-white text-gray-500'>Non sei ancora registrato? Clicca <a href="#" className='underline' onClick={() => {setSubscribe(true)}}>qui</a>!</span>}
                            <div>
                               
                            </div>
                            <div className="flex flex-col items-center justify-evenly gap-3">
                                {
                                    subscribe ?
                                    <input type="submit" className="btn-standard mt-4 hover:scale-[110%] transform transition duration-300 items-center rounded-2xl bg-sky-200 hover:bg-sky-400 dark:text-white dark:bg-sky-800 p-3 pr-5 pl-5 hover:dark:bg-sky-900 hover:text-white" value="Registrati" />
                                    : <input type="submit" className="btn-standard mt-4 hover:scale-[110%] transform transition duration-300 items-center rounded-2xl bg-sky-200 hover:bg-sky-400 dark:text-white dark:bg-sky-800 p-3 pr-5 pl-5 hover:dark:bg-sky-900 hover:text-white" value="Login" />
                                    
                                }
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </>
    )
}

export default Login