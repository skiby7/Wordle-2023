import { useEffect, useState } from "react";
import axios from "axios";
import { host } from "./Global";

const Ranking = ({user, setAuth}) => {
	const [ranking, setRanking] = useState([]);
	useEffect(() => {  
		axios.get(`${host}/showMeRanking?username=${user.username}`).then(
			function(response){
				setRanking(response.data.ranking);
				console.log(response.data.ranking);
				if (response.status === 401) {
					setAuth(false);
				}
				
			}).catch(function(error){
				if (error.response.status === 401) {
					setAuth(false);
				}
			})
	}, []);
	return (
	   <div className="flex flex-col w-screen h-screen">
                <div className='h-screen flex flex-row items-center justify-center'>
				    <div className='flex flex-col items-center justify-evenly w-full max-w-2xl bg-gray-100 dark:bg-gray-800 rounded-lg shadow-xl py-10 px-10'>
					<h1 className="text-black dark:text-white font-mono text-[2em] mb-8">🏅 Classifica Top 5 🏅</h1>
					<Names users={ranking}/>

                        {/* <div className="flex flex-col gap-3 justify-evenly items-center max-w-full w-full" >
                            
                        </div> */}
                    </div>
                </div>
            </div>
	);
	
  };


  const Names = ({users}) => {
	const items = [];
	for (let i = 0; i < users.length && i < 5; i++) {
		let string = "";
		if (i === 0 ) string = '🥇 - ' + users[i]; 
		else if (i === 1 ) string = '🥈 - ' + users[i]; 
		else if (i === 2 ) string = '🥉 - ' + users[i]; 
		else string = i + 1 + ' - ' + users[i]; 
        items.push(<div className="flex font-mono text-[1.5em] hover:scale-[120%] transform transition duration-300 bg-gray-100 dark:bg-slate-600 w-[15em] rounded-xl justify-left shadow-lg px-14 py-5">{string}</div>)
    }
	return (
	<div className="flex flex-col gap-5">
		 {items}
	</div>);

}

 


export default Ranking;