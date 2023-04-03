import React, { useEffect, useState } from 'react';
// import DataTable from 'react-data-table-component';
import { toast } from 'react-hot-toast';
import Game from './Game';
import './spinner.css';
import axios from 'axios';
import { BounceLoader } from 'react-spinners'
import { host } from './Global'
function Home({user, setUser, isPlaying, setIsPlaying, setAuth, isDarkMode}) {
	const [data, setData] = useState({
		details: "",
		gamesPlayed: 0,
		gamesWonPct: 0,
		lastStreak: 0,
		maxStreak: 0,
		guessDistribution: []
	});
	const [loading, setLoading] = useState(false);
  
  const getData = () => {
		axios.get(`${host}/sendMeStatistics?username=${user.username}`).then(
			function (response) {
				if (response.status === 401) {
					setAuth(false);
					return;
				}
				setData(response.data);
			}
		)
	  
  }

  const routine = () => {
	isUserPlaying();
	getData();
  }


  const isUserPlaying = () => { 
	axios.get(`${host}/getGameStatus?username=${user.username}`).then(
		function(response){
			response.data.isPlaying === true ? setIsPlaying(true) : setIsPlaying(false);
			// if (response.data.isPlaying === true){
			  setUser({username: user.username, token: user.token, wordId: response.data.wordId})
			// } 
			if (response.status === 401) {
				setAuth(false);
			}
			
		}).catch(function(error){
			if (error.response.status === 401) {
				setAuth(false);
			}
		})
	}

	

  useEffect(() => {
	getData();
	const updateGameStatus = setInterval(() => {
		routine();
	}, 5000)
	return () => clearInterval(updateGameStatus);
	// eslint-disable-next-line react-hooks/exhaustive-deps
  },[])
 
 
  const startGame = () => { 
	setLoading(true);
	axios.post(`${host}/playWordle?username=${user.username}`).then(
		function(response){
			if (response.status === 401) {
				setAuth(false);
				return;
			} 
			if(response.status === 200) {
				setUser({username: user.username, token: user.token, wordId: response.data.wordId});
			} else { 
				toast.error(response.details);
			}
		}).catch(function(error){
			if (error.response.status === 401) {
				setAuth(false);
			} else if (error.response.status === 400) {
				console.log(error)
				if (error.response.data.victory) toast("Hai già vinto, aspetta l'estrazione di una nuova parola!", {icon : '🚀'})				
				else toast("Gioco chiuso, aspetta l'estrazione di una nuova parola!", {icon : '🔚'})	
				setLoading(false)			
			}
		})
	}


 
  
  return (
	<>
	<div className='flex flex-col min-h-screen justify-center'>
	<Navbar username={user.username} token={user.token} gamesPlayed={data.gamesPlayed} gamesWonPct={data.gamesWonPct} lastStreak={data.lastStreak} maxStreak={data.maxStreak} currentWordId={user.wordId}/>
	{loading ? <BounceLoader color={"#0ea5e9"} className='flex ml-[120px] mb-10 border-cyan-400'></BounceLoader> : <div></div>}
	{!isPlaying ? <button onClick={startGame} className='relative ml-20 pl-5 pr-5 pt-3 pb-3 rounded-xl bg-sky-300 hover:bg-sky-400 dark:text-white dark:bg-sky-800  hover:dark:bg-sky-900 hover:text-white'>Inizia una partita!</button> 
				: <Game username={user.username} token={user.token} wordId={user.wordId} setAuth={setAuth} setLoading={setLoading}></Game>}
	</div>
	  </>
  );
}

function msToTime(s) {
	const ms = s % 1000;
	s = (s - ms) / 1000;
	const secs = s % 60;
	s = (s - secs) / 60;
	const mins = s % 60;
	// const hrs = (s - mins) / 60;
  
	return mins.toLocaleString('it-IT', {minimumIntegerDigits:2, useGrouping: false}) + ':' + secs.toLocaleString('it-IT', {minimumIntegerDigits:2, useGrouping: false})
  }

const Navbar = ({username, token, gamesPlayed, gamesWonPct, lastStreak, maxStreak, currentWordId}) => {
	return (
		<>
		<div className="fixed top-0 left-0 h-20 w-screen justify-center gap-7 items-center
		flex flex-row bg-gray-200
		dark:bg-gray-900 text-white shadow-md -z-40 pl-24">
			<span className='navbar-labels'>Benvenuto <span className='navbar-labels-highlight'>{username}</span>!</span>
			<span className='navbar-labels'>Partite giocate: <span className='navbar-labels-highlight'>{gamesPlayed}</span></span>
			<span className='navbar-labels'>Percentuale vittoria: <span className='navbar-labels-highlight'>{(parseFloat(gamesWonPct) * 100).toFixed(2)}%</span></span>
			<span className='navbar-labels'>Ultima streak: <span className='navbar-labels-highlight'>{lastStreak}</span></span>
			<span className='navbar-labels'>Streak più lunga: <span className='navbar-labels-highlight'>{maxStreak}</span></span>
			<span className='navbar-labels'>Parola numero: <span className='navbar-labels-highlight'>{currentWordId === -1 ? "None" : currentWordId}</span></span>
			<span className='navbar-labels'>Timer: <span className='navbar-labels-highlight'><Timer username={username} token={token}/></span></span>
		</div>
		</>
	)
}

const Timer = ({username, token}) => {
	const[timer, setTimer] = useState(-1);
	const syncWordTime = () => { 
		axios.defaults.headers.common['Authorization'] = 'Bearer ' + token
		axios.get(`${host}/wordTimer?username=${username}`).then(
			function(response){
				setTimer(response.data.time);
			}).catch(function(error){
				
			});
		console.log(`${host}/wordTimer?username=${username}&token=${token}`);
	}

	useEffect(() => {
		// syncWordTime();
		const updateTimer = setInterval(() => {
			if (timer - 1000 >= 0) setTimer(timer - 1000);
			else setTimer(0)
			const now = new Date();
			if (now.getSeconds() % 10 === 0 || timer < 0) {
				syncWordTime();
			}
		}, 1000);
		return () => clearInterval(updateTimer);
	  }, [timer])
    return (
        <>
           <span>{msToTime(timer)}</span>
        </>
    )
}
export default Home; 
   