import { BsBoxArrowLeft } from 'react-icons/bs';
import { FaSun, FaMoon, FaGamepad, FaStar} from 'react-icons/fa';
import React, { useState, useEffect } from 'react';
import {Link} from "react-router-dom";
import { toast } from 'react-hot-toast';
import { host } from './Global'
import qs from 'qs';
import axios from 'axios';

const Sidebar = ({user, setAuth, setIsDarkMode}) => {
	const [darkMode, setDarkMode] = useState(true);
	const [modeIcon, setModeIcon] = useState(<FaSun size="20"/>);
	const setMode = () => {
		setDarkMode(!darkMode)
		setIsDarkMode(!darkMode)
		if (darkMode) {
		  document.documentElement.classList.add('dark');
		  setModeIcon(<FaMoon size="20"/>);
		} else {
		  document.documentElement.classList.remove('dark');
		  setModeIcon(<FaSun size="20"/>);
		}
	  }
	useEffect(() => {  
		setMode();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	const deAuth = () => {
		axios.post(`${host}/logout`, 
		qs.stringify({ 'username' : user.username})).then((response) => {if (response.status === 200) {setAuth(false)} else {toast.error('Errore logout!')}})
			
	}

	return (
	  <div className="fixed top-0 left-0 h-screen w-20 m-0
					  flex flex-col bg-gray-200
					  dark:bg-gray-900 text-white ">

		<Link to="/"><SideBarIcon text={'Gioca! 🚀'} icon={<FaGamepad size="25" />} /></Link>
		<Link to="/ranking"><SideBarIcon text={'Classifica 🏅'} icon={<FaStar size="25" />}  /></Link>

		<div className='flex flex-col mt-auto mb-4 mx-auto'>
		<SideBarIcon icon={modeIcon} onClickEvent={setMode} text={darkMode ? 'Imposta modalità scura 🌚' : 'Imposta modalità chiara ☀️'} />
      	<SideBarIcon text={"Esci 🚶"} icon={<BsBoxArrowLeft  size="25" className='pr-1'/>} onClickEvent={deAuth}/>
		</div>
	  </div>
	);
	
  };

  const SideBarIcon = ({ icon, text = 'tooltip 💡', onClickEvent = () =>{}, props = null}) => {
	// const [times, setTimes] = useState(0); // every instance of this component has its state
	return (
	<div className="sidebar-icon group" onClick={onClickEvent}>
	  {icon}
	  <span className="sidebar-tooltip group-hover:scale-100 z-20">{text}</span>
	</div>
	);
  };
 

export default Sidebar;