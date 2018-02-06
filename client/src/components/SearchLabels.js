import React, { Component } from 'react';
import { Auth } from 'aws-amplify';
import axios from 'axios';
import $ from 'jquery';
import '../styles/searchLabels.css';
import preview from '../img/preview.jpg';
import Loader from './Loader';
import SearchResults from './SearchResults'


class SearchLabels extends Component {
	constructor(props) {
	    super(props);
	    this.onSearch = this.onSearch.bind(this);
	}

	componentDidMount() {
		this.setState({
			results:[],
			isLoading:false
		});
	}

	onSearch(e) {
		var self = this;
		e.preventDefault();
		self.setState({
			results: [],
			isLoading: true
		})

      	var searchValue = document.getElementById("search-value").value;
		var idToken = Auth.credentials.params.Logins["cognito-idp.us-east-1.amazonaws.com/us-east-1_BIhRQnDpw"];
		axios.post('https://1rksbard2i.execute-api.us-east-1.amazonaws.com/prod/picture/search/', "", {
		    headers: {
		    	"Authorization": idToken,
		    	"search-key": searchValue
		    }
		  })
		  .then(function (response) {
		    if(response.data.pictures.length != 0) {
		    	console.log(response);
		    	self.setState({
		    		results: response.data.pictures, 
		    		isLoading: false
		    	}); 	
		    } 
		    else {
		    	self.setState({
		    		results: [], 
		    		isLoading: false
		    	}); 
		    	console.log("no pictures found");
		    }
		  })
		  .catch(function (error) {
		    console.log(error);
		  });

	}

	render(){
		var results = [];
		var isLoading = false;
		if(this.state) { 
			results = this.state.results;
			isLoading = this.state.isLoading;
		};

		return(
			<div>
				<form id="search-form" onSubmit={this.onSearch} className="form-inline">
				    <div className="form-group has-feedback col-xs-offset-1 col-xs-10">
	                  <input className="form-control col-xs-12" id="search-value" placeholder="Search Labels" type="text" />
	                  <span className="form-control-feedback glyphicon glyphicon-search"></span>
	                </div>
                </form>
                { isLoading && <Loader title="Loading"/> }
                <SearchResults results={results} />
			</div>
		);
	}
}

export default SearchLabels;