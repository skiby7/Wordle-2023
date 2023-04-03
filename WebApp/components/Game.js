import { useEffect, useState } from "react";
import { LoaderIcon, toast } from "react-hot-toast";
import './spinner.css'
import { host } from './Global'
import axios from 'axios';
import qs from 'qs';
const Game = ({username, token, wordId, setAuth, setLoading}) => {
	const initialList = Array.from({length: 12}, () => ["", "", "", "", "", "", "", "", "", ""]);
	const [guesses, setGuesses] = useState(initialList);
	const [hints, setHints] = useState(initialList);
	const rows = [];
	const [word, setWord] = useState("");
    const onChange = (e) => {
		if(e.target.value.length <= 10){
        	setWord(e.target.value.trim().toLowerCase());
		}
    }

	const onSubmit = async (e) => {
        e.preventDefault();

		if(word.length !== 10){
			toast.error('La parola deve contenere 10 caratteri!')
			return;
		}
		
		await axios.post(`${host}/sendWord`, qs.stringify({'username' : username, 'word' : word, 'wordId' : wordId})).then(
			function(response) {
				if (response.data.victory){
					toast('Vittoria!', {icon: '🥳'});
					toast('Traduzione:  ' + response.data.translatedWord, {icon: '🌐', duration: 7000 })
				} else if (!response.data.wordExists) {
					toast('La parola non esiste nel dizionario, prova di nuovo!', {icon : '🧐'});
				}else if (response.data.wordExists) {
					toast.error('Sbagliato, prova di nuovo!');
				} else if (response.status === 400) {
					toast.error(response.data.details);
				}
				fetchData();
			}
		);
		
		
		
	}
	
	
	const fetchData = () => {
		axios.get(`${host}/getGameHistory?username=${username}&wordId=${wordId}`)
		.then(
			function(response){
				if (response.status === 401) {
					setAuth(false);
					return;
				}
				if(response.status === 200) {
					let rawGuesses = Array.from(response.data.guesses.split(':'));
					let tmp = [];
					let splittedGuesses = []
					let i = 0, j = 0;
					for(; i < rawGuesses.length; i++) {
						tmp = [];
						for(j = 0; j < 10; j++) {
							tmp.push(rawGuesses[i][j]);
						}
						splittedGuesses[i] = [...tmp];
					}
					for(; i < 12; i++) {
						splittedGuesses[i] = Array(10).fill('');
					}
					setGuesses(splittedGuesses);

					let rawHints = Array.from(response.data.hints.split(':'));
					console.log(rawHints)
					tmp = [];
					let splittedHints = []
					i = 0;
					for(; i < rawHints.length; i++) {
						tmp = []
						for(j = 0; j < 10; j++) {
							tmp.push(rawHints[i][j]);
						}
						splittedHints[i] = [...tmp];
					}
					for(; i < 12; i++) {
						splittedHints[i] = Array(10).fill('');
					}
					setHints(splittedHints);
			 	} 
				 
			}).catch(function(error){
				if (error.response && error.response.status === 401) {
					setAuth(false);
				}
			})

	}
	useEffect(() => {
		fetchData();
		setLoading(false);

	}, [wordId])

	for (let i = 0; i < 12; i++) {
		rows.push(<Row key={i.toString()} className="flex" guess={guesses[i]} hint={hints[i]}/>)
	}
	return (
		<>
	
		<div className="flex flex-col gap-1 mb-5 mt-24">
			{rows}
		</div>

		<form className="flex flex-col justify-evenly items-center max-w-full w-full" onSubmit={onSubmit}>
			<div>
				<input placeholder='Indovina la parola' type="text" className='font-mono text-md w-96 p-2 bg-gray-200 text-primary rounded-md outline-none transition duration-150 ease-in-out shadow-2xl' name="word" value={word} onChange={e => onChange(e)} />
			</div>
			<div>
				<input type="submit" className="btn-standard mt-4 items-center rounded-2xl bg-sky-200 hover:bg-sky-400 dark:text-white dark:bg-sky-800 p-3 pr-5 pl-5 hover:dark:bg-sky-900 hover:text-white" value="Invia" />
			</div>
			</form>
		</>
	);


}

const Row = ({guess, hint}) => {
	const items = [];
	for (let i = 0; i < guess.length; i++) {
        items.push(<Tile key={i.toString()} className="flex" letter={guess[i]} state={hint[i]}/>)
    }
	return (
	<div className="flex flex-row gap-1">
		 {items}
	</div>);

}

const Tile = ({letter, state}) => {
	const getBgColor = (s) => {
		switch(s) {
			case '':
				return "not-in-word";
			case '?': 
				return "not-in-place";
			case '+':
				return "correct";
			default:
				return "not-in-word"
		}
	}
	return (
		<div className={"h-12 w-12 border flex m-0 dark:border-gray-50 border-gray-700 rounded-md " + getBgColor(state)}><div className="m-auto">{letter}</div></div>
	);
} 

export default Game;