function makeCityRequests(){
	var uri = currentEvent.location.dbpedia_res_city;
	currentCity = {};
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCity.description = result.results.bindings[0].res.value;
		}else{
			sparqlRequest(uri, "http://www.w3.org/2000/01/rdf-schema#comment", true).done(function(result) {
				if(result.results.bindings[0] != undefined){	
					currentCity.description = result.results.bindings[0].res.value;
					infoboxCity();
				}
			});
		}
		infoboxCity();
	});
					
	sparqlRequest(uri, "http://dbpedia.org/ontology/populationTotal", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCity.population = result.results.bindings[0].res.value;
		}
		infoboxCity();
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/areaTotal", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCity.area = result.results.bindings[0].res.value;
		}
		infoboxCity();
	});
	
	sparqlRequest(uri, "http://xmlns.com/foaf/0.1/depiction", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCity.image = result.results.bindings[0].res.value;
			addImage(currentCity.image);
		}
	});
}

function makeRegionRequests(){
	var uri = currentEvent.location.dbpedia_res_region;
	currentRegion = {};
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		if(result.results.bindings[0] != undefined){	
			currentRegion.description = result.results.bindings[0].res.value;
		}else{
			sparqlRequest(uri, "http://www.w3.org/2000/01/rdf-schema#comment", true).done(function(result) {
				if(result.results.bindings[0] != undefined){	
					currentRegion.description = result.results.bindings[0].res.value;
					infoboxRegion();
				}
			});
		}
		infoboxRegion();
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/populationDensity", false).done(function(result) {
		if(result.results.bindings[0] != undefined){			
			currentRegion.populationDensity = result.results.bindings[0].res.value;
			infoboxRegion();
		}
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/areaLand", false).done(function(result) {
		if(result.results.bindings[0] != undefined){	
			currentRegion.area = result.results.bindings[0].res.value;
			infoboxRegion();
		}
	});
	sparqlRequest(uri, "http://xmlns.com/foaf/0.1/depiction", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentRegion.image = result.results.bindings[0].res.value;
			addImage(currentRegion.image);
		}
	});
}

function makeCountryRequests(){
	var uri = currentEvent.location.dbpedia_res_country;
	currentCountry = {};
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCountry.description = result.results.bindings[0].res.value;
		}else{
			sparqlRequest(uri, "http://www.w3.org/2000/01/rdf-schema#comment", true).done(function(result) {
				if(result.results.bindings[0] != undefined){	
					currentCoutnry.description = result.results.bindings[0].res.value;
					infoboxCountry();
				}
			});
		}
		infoboxCountry();
	});
	
	sparqlRequestWithHTML(uri, "http://dbpedia.org/ontology/capital").done(function(result) {
		sparqlRequest(result.results.bindings[0].res.value, "http://dbpedia.org/property/name", true).done(function(result){
			if(result.results.bindings[0] != undefined){
				currentCountry.capital = result.results.bindings[0].res.value;
			}
			infoboxCountry();
		});
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/PopulatedPlace/areaTotal", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCountry.populationDensity = result.results.bindings[0].res.value;
		}
		infoboxCountry();
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/areaTotal", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCountry.area = result.results.bindings[0].res.value;
		}
		infoboxCountry();
	});
	
	sparqlRequest(uri, "http://xmlns.com/foaf/0.1/depiction", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentCountry.image = result.results.bindings[0].res.value;
			addImage(currentCountry.image);
		}
	});
}

function makeBandRequests(band){
	
	var uri = band.dbpedia_resource;
	currentBand = { "name" : band.name,
					"members" : band.members };
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		
		if(result.results.bindings[0] != undefined){
			currentBand.description = result.results.bindings[0].res.value;
		}else{
			sparqlRequest(uri, "http://www.w3.org/2000/01/rdf-schema#comment", true).done(function(result) {
				if(result.results.bindings[0] != undefined){	
					currentBand.description = result.results.bindings[0].res.value;
					infoboxBand();
				}
			});
		}
		infoboxBand();
	});
	
	sparqlRequest(uri, "http://xmlns.com/foaf/0.1/depiction", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentBand.image = result.results.bindings[0].res.value;
			addImage(currentBand.image);
		}
	});
}
function makeMemberRequests(member){
	var uri = member.dbpedia_resource;
	currentMember = { "name" : member.name };
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentMember.description = result.results.bindings[0].res.value;
		}else{
			sparqlRequest(uri, "http://www.w3.org/2000/01/rdf-schema#comment", true).done(function(result) {
				if(result.results.bindings[0] != undefined){	
					currentMember.description = result.results.bindings[0].res.value;
					infoboxMember();
				}
			});
		}
		infoboxMember();
	});
	
	sparqlRequest(uri, "http://xmlns.com/foaf/0.1/depiction", false).done(function(result) {
		if(result.results.bindings[0] != undefined){
			currentMember.image = result.results.bindings[0].res.value;
			addImage(currentMember.image);
		}
	});
}

function sparqlRequest(uri, predicate, string){
	var url = "http://dbpedia.org/sparql";
	var query = "";
	if(string){				
		var query = [
			"select (str(?o) AS ?res) where{ ",
			"<" + uri + "> <" + predicate +"> ?o",
			"FILTER (langMatches(lang(?o),'en'))",
			"}"
		].join(" ");
	}else{
		var query = [
			"select (?o AS ?res) where{ ",
			"<" + uri + "> <" + predicate +"> ?o",
			"}"
		].join(" ");
	}
	var queryUrl = url+"?query="+ encodeURIComponent(query) +"&format=json";
	return $.ajax({
		dataType: "jsonp",  
		url: queryUrl
	});
}

function sparqlRequestWithHTML(uri, predicate){
	var url = "http://dbpedia.org/sparql";
	var query = "";		
	var query = [
		"select (?o AS ?res) where{ ",
		"<" + uri + "> <" + predicate +"> ?o",
		"}"
	].join(" ");
	
	var queryUrl = url+"?query="+ encodeURIComponent(query) +"&format=json";
	return $.ajax({
		dataType: "jsonp",  
		url: queryUrl
	});
}