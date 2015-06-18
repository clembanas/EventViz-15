var map;
var tree;
var showInfo = false;
var hoverCountry = null;
var onGreyLayer = false;
var visibleMarkers = [];

//set icons for events
var redIconHover = L.icon({
	iconUrl: 'images/RedDotHover.png',
	iconSize : [ 12, 12 ],
	iconAnchor : [ 6, 6 ]
});
var redIcon = L.icon({
	iconUrl : 'images/RedDot.png',
	iconSize : [ 12, 12 ],
	iconAnchor : [ 6, 6 ]
});

var currentEvent = {};

var currentCity = null;
var currentRegion = null;
var currentCountry = null;
var currentBand = {};
var currentMember = {};

var currentMarkers = [];

//define sytles for country layers
var normalStyle = {
	color : '#0033ff',
	weight : 1,
	opacity : 0.1,
	fillOpacity : 0.01,
	fillColor : '#0033ff'
};		
var highlightStyle = {
	color : '#0033ff',
	weight : 1,
	opacity : 0.4,
	fillOpacity : 0.2,
	fillColor : '#0033ff'
};		
var activeStyle = {
	color : '#0033ff',
	weight : 1,
	opacity : 0.2,
	fillOpacity : 0.1,
	fillColor : '#0033ff'
};
var greyStyle = {
	color : '#808080',
	weight : 0,
	opacity : 0.8,
	fillOpacity : 0.8,
	fillColor : '#808080'
};

var currentActiveCountries = [];
var countries = [];

function isMarkerVisible(marker){
	var coordinates = [ 
	                   [map.getBounds()._southWest.lat, map.getBounds()._southWest.lng], 
	                   [map.getBounds()._southWest.lat, map.getBounds()._northEast.lng], 
	                   [map.getBounds()._northEast.lat, map.getBounds()._northEast.lng], 
	                   [map.getBounds()._northEast.lat, map.getBounds()._southWest.lng]
	                   ];
	return isInCoordinates(coordinates, marker._latlng.lng, marker._latlng.lat);
}

function isOverlapping(bounds){
	var mapBounds = map.getBounds();
	
	return !(bounds._southWest.lat > mapBounds._northEast.lat ||bounds._southWest.lng > mapBounds._northEast.lng ||
			bounds._northEast.lat < mapBounds._southWest.lat || bounds._northEast.lng < mapBounds._southWest.lng);
	
}

function containsEvent(arr, id){
	var event = null;
	for(var i = 0; i<arr.length; i++){
		if(arr[i].event.id == id){
			event = arr[i];
			break;
		}
	}
	return event;
}

function addEventToList(event){
	$("#floatingList").append($("<li id="+event.event.id+">").text(event.event.eventName));
	$("#" + event.event.id).click({event: event}, function(e, data){
		for(var i = 0; i < visibleMarkers.length; i++){
			for(var j = 0; j<visibleMarkers[i].options.ids.length; j++){
				if(visibleMarkers[i].options.ids[j] == event.event.id){
					clickMarker(visibleMarkers[i], event);
					break;
				}
			}
		}
	}).hover(function(e){
		$(this).css("background-Color", "#F4FA58");
	}).mouseout(function(e){
		$(this).css("background-Color", "#FFFFFF");
	});
}

function getAllVisibleEvents(zoom){	
	for(var i = 0; i < visibleMarkers.length; i++){
		if(visibleMarkers[i].options.markers  == undefined){
			visibleMarkers[i].options.markers = [];
			for(var j = 0; j < visibleMarkers[i].options.ids.length; j++){
				if(map._zoom == zoom){
					$.ajax({
				    	url: "/getEventById",
				    	async: false,
				    	data: { id: visibleMarkers[i].options.ids[j]} ,
				    	dataType: 'json',
				    	success: function(result) {
				    		visibleMarkers[i].options.markers.push(result);

							//if zoom level is still correct
							if(map._zoom == zoom){						
								addEventToList(result);
							}
				    	}
				    });
				}
				else{
					break;
				}
			}
		}
		else{
			for(var j = 0; j < visibleMarkers[i].options.markers.length; j++){
				if(map._zoom == zoom){						
					addEventToList(visibleMarkers[i].options.markers[j]);
				}
			}
		}
	}
}

function updateFloatingInfobox(){
	checkIfMarkersVisible();
	$("#floatingList").empty();
	getAllVisibleEvents(map._zoom);
}

function checkIfMarkersVisible(){
	visibleMarkers = [];
	iterateMarkers(tree._topClusterLevel);
}


//not used
function iterateMarkers(tmpMarkers){
	if(tmpMarkers._zoom  < map._zoom){
		for(var i = 0; i < tmpMarkers._childClusters.length; i++){
			if(isOverlapping(tmpMarkers._bounds)){
				iterateMarkers(tmpMarkers._childClusters[i]);
			}
		}
		for(var i =0; i < tmpMarkers._markers.length; i++){
			if(isMarkerVisible(tmpMarkers._markers[i])){
				visibleMarkers.push(tmpMarkers._markers[i]);
			}
		}
	}
}

function updateMarkers(){
	for(var i = 0; i<currentMarkers.length; i++){
		if(currentActiveCountries.length == 0){
			if(!map.hasLayer(currentMarkers[i])){
				currentMarkers[i].addTo(map);
			}
		}
		else{					
			if(!isInActiveCountry(currentMarkers[i].options.country)){
				map.removeLayer(currentMarkers[i]);
			}
			else if(!map.hasLayer(currentMarkers[i])){
				currentMarkers[i].addTo(map);
			}
		}
	}
}

function doBlink(id, count) {
    $(id).animate({ backgroundColor: "#F4FA58" }, {
        duration: 100, 
        complete: function() {
            $(id).delay(100).animate({ backgroundColor: "#ffffff" }, {
                duration: 100,
                complete: function() {
                    if(count > 1) {
                        doBlink(id, --count);
                    }
                }
            });
        }
    });
}

function isInActiveCountry(eventMarker){
	var inside = false;
	for(var i = 0; i < currentActiveCountries.length; i++){
		if(eventMarker === currentActiveCountries[i]){
			inside = true;
		}
	}
	return inside;
}

/*
function isInCountry(countryName, eventMarker) {
	inside = false;
	for(var m = 0; m< coordinates.length; m++){
		var inside = isInCoordinates(e.target.feature.geometry.type == "Polygon" ? coordinates[m] : coordinates[m][0], eventMarker._latlng.lat, eventMarker._latlng.lng);
		 if(inside){
			 break;
		 }
	}
	return inside;
}
*/

function addImage(imgURI){
	$("#img").append($("<img id=image class=image src='" + imgURI + "?width=250'>"));
}

function replaceCharacters(word){
	if(word.indexOf(',') != -1){
		word = word.substring(0, word.indexOf(','));
	}
	if(word.indexOf('-') != -1){
		word = word.substring(0, word.indexOf('-'));
	}
	if(word.indexOf('.') != -1){
		word = word.substring(0, word.indexOf('.'));
	}
	word = word.replace(/ã/g, 'a');
	word = word.replace(/é/g, 'e');
	word = word.replace(/á/g, 'a');
	word = word.replace(/ó/g, 'o');
	word = word.replace(/í/g, 'i');
	word = word.replace(/ú/g, 'u');
	word = word.replace(/[^a-zA-Z0-9 ]/gmi, '');
	while(word.substring(word.length-1) == ' '){
		word = word.substring(0, word.length-1);
	}
	
	return word;
}

function processEvent(result){
	console.log(result);
	currentEvent = result;
	currentCity = null;
	currentRegion = null;
	currentCountry = null;
	infoboxEvent();
	var terms = [];
	terms.push(replaceCharacters(currentEvent.event.eventName));
	for(var i = 0; i < currentEvent.bands.length; i++){
		var word = replaceCharacters(currentEvent.bands[i].name);
		if($.inArray(word, terms ) == -1){				
			terms.push(word);
		}
	}
	var word = replaceCharacters(currentEvent.location.locationName);
	if($.inArray(word, terms ) == -1){				
		terms.push(word);
	}
	$.getJSON( "/getSentiment", { terms: JSON.stringify(terms), location: replaceCharacters(currentEvent.location.cityName) } ).done(function(result){
		$("#socialInfoP").html("Sentiment data");
		$("#socialLoading").css("visibility", "hidden");					
		if(!$.isEmptyObject(result) && $("#social").css("visibility") == "visible"){
			$("#socialChart").css("visibility", "visible");			
			$("#socialResult").css("visibility", "visible");
			$("#socialResult").empty();
			
			$("#socialResult").append($("<div id=passionDiv class=socialDiv>"));
			$("#passionDiv").append($("<p class=socialDesc>").text("Passion:"));
			$("#passionDiv").append($("<p class=socialValue>").text(result.score_passion));
			
			$("#socialResult").append($("<div id=reachDiv class=socialDiv>"));
			$("#reachDiv").append($("<p class=socialDesc>").text("Reach:"));
			$("#reachDiv").append($("<p class=socialValue>").text(result.score_reach));
			
			$("#socialResult").append($("<div id=sentimentDiv class=socialDiv>"));
			$("#sentimentDiv").append($("<p class=socialDesc>").text("Sentiment:"));
			$("#sentimentDiv").append($("<p class=socialValue>").text(result.score_sentiment));
			
			$("#socialResult").append($("<div id=strengthDiv class=socialDiv>"));
			$("#strengthDiv").append($("<p class=socialDesc>").text("Strength:"));
			$("#strengthDiv").append($("<p class=socialValue>").text(result.score_strength));
			
			$("#socialResult").append($("<div id=keywordsDiv class=socialDiv>"));
			$("#keywordsDiv").append($("<p class=socialDesc>").text("Keywords:"));
			if(result.keywords.length != 0){
				for(var i = 0; i < result.keywords.length; i++){
					$("#socialResult").append($("<div class=socialDivMarginLeft>").append($("<p class=socialValue>").text(result.keywords[i].keyword + "\t" + result.keywords[i].occurance)));
				}
			}else{
				$("#keywordsDiv").append($("<p class=socialValue>").text("No keywords found."));
			}
			
			$("#socialResult").append($("<div id=hashtagsDiv class=socialDiv>"));
			$("#hashtagsDiv").append($("<p class=socialDesc>").text("Hashtags:"));
			if(result.hashtags.length != 0){
				for(var i = 0; i < result.hashtags.length; i++){
					$("#socialResult").append($("<div class=socialDivMarginLeft>").append($("<p class=socialValue>").text(result.hashtags[i].hashtag + "\t" + result.hashtags[i].occurance)));
				}
			}else{
				$("#hashtagsDiv").append($("<p class=socialValue>").text("No hashtags found."));
			}
			
			var totalSentiment = result.sentiment.positive + result.sentiment.negative;
			
			if(totalSentiment){
				$("#socialResult").css("width", "50%");
				var chart = new Highcharts.Chart({
					chart: {
						plotBackgroundColor: null,
						plotBorderWidth: null,
						plotShadow: false,
						renderTo: 'socialChart'
					},
					title: {
						text: 'Sentiment'
					},
					tooltip: {
						pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
					},
					series: [{
						type: 'pie',
						name: 'Sentiment',
						data: [
						       ['positive',   result.sentiment.positive / totalSentiment * 100],
						       ['negative',       result.sentiment.negative / totalSentiment * 100]
						       ]
					}]
				});
			}
			else{
				$("#socialResult").css("width", "100%");
			}
		}
	});
}

function clickMarker(marker, event){
	showInfo = true;
	removeCountries();
	jQuery.each($("#navEvent").nextAll(), function(){
		$(this).remove();
	});
	var options = marker.target == undefined ? marker.options : marker.target.options;
	var tmpLatLng = marker.latlng == undefined ?  $.extend( {}, marker._latlng  ): $.extend({}, marker.latlng  );
	if(options.ids.length == 1 || event != undefined){
		tmpLatLng.lng += 0.01021508561708;
		tmpLatLng.lat -= 0.003382020592895;
		map.setView(tmpLatLng, 16);
		map.setView(tmpLatLng, 16);
		$("#infobox").css("visibility", "visible");
		if($("#floatingInfo").css("visibility") === "visible"){				
			$("#floatingInfo").animate({"width": "-=20%" }, 200, function(){
				$(this).css("visibility", "hidden");
			});
		}
		
		$("#social").css("visibility", "visible");
		$("#socialInfo").css("visibility", "visible");
		$("#socialResult").empty();
		$("#socialChart").empty();
		$("#socialLoading").empty();
		$("#socialLoading").css("visibility", "visible");
		var opts = {
				lines: 13, // The number of lines to draw
				length: 20, // The length of each line
				width: 10, // The line thickness
				radius: 20, // The radius of the inner circle
				scale: 1, // Scales overall size of the spinner
				corners: 1, // Corner roundness (0..1)
				rotate: 0, // The rotation offset
				direction: 1, // 1: clockwise, -1: counterclockwise
				color: '#000', // #rgb or #rrggbb or array of colors
				speed: 1, // Rounds per second
				trail: 60, // Afterglow percentage
				shadow: false, // Whether to render a shadow
				hwaccel: false, // Whether to use hardware acceleration
				className: 'spinner', // The CSS class to assign to the spinner
				zIndex: 2e9, // The z-index (defaults to 2000000000)
				top: '50%', // Top position relative to parent
				left: '50%' // Left position relative to parent
		};
		var loadingDiv = document.getElementById("socialLoading");
		var spinner = new Spinner(opts).spin(loadingDiv);
		$("#socialInfoP").html("Sentiment data is processed");
		if(event == undefined){		
			if(options.markers != undefined){
				processEvent(options.markers[0]);
			}
			else{		
				$.getJSON( "/getEventById", { id : options.ids[0] }).done(function(result){
					processEvent(result);
				});
			}
		}
		else{
			processEvent(event);
		}
	}else{
		if($("#infobox").css("visibility") == "visible"){
			showInfo = false;
			$("#infobox").css("visibility", "hidden");
			$("#social").css("visibility", "hidden");
			$("#socialInfo").css("visibility", "hidden");
			$("#socialResult").css("visibility", "hidden");
			$("#socialLoading").css("visibility", "hidden");
			$("#socialChart").empty();
			$("#socialChart").css("visibility", "hidden");
			$("#floatingInfo").css("visibility", "visible");
			$("#floatingInfo").animate({"width": "+=20%"}, 200);
			updateFloatingInfobox();
		}
		map.setView(tmpLatLng, 16);
		if(marker.target._popup != undefined){
			marker.target.unbindPopup();
		}
		var string = "<b>Events</b>";
//		$.when({marker : marker, markers : options.markers}, function(m){
//			console.log(m);
//			var deferred = jQuery.Deferred();
//			for(var i = 0; i < (m.markers.length > 15 ? 15 : m.markers.length); i++){
//				var pID = m.markers[i].event.id + "p";
//				string += "<p id=" + pID + ">" + m.markers[i].event.eventName + "</p>";
//				$("#map").on("click", "#" + pID, {event: m.markers[i]}, function(e){
//					m.marker.target.closePopup();
//					clickMarker(m.marker, e.data.event);
//					
//				}).on("hover", "#" + pID, function(e){
//					$(this).css("background-Color", "#F4FA58");
//				}).on("mouseout", "#" + pID, function(e){
//					$(this).css("background-Color", "#FFFFFF");
//				});
//			}
//			return deferred.promise();
//		}).then(function(m){
//			console.log(m);
//			var deferred = jQuery.Deferred();
//			if(m.markers.length > 15){
//				string += "<p> +" + (m.markers.length - 15) + " other events</p>";
//			}
//			m.marker.target.bindPopup(string).openPopup();
//		});
		
		
		for(var i = 0; i < (options.markers.length > 15 ? 15 : options.markers.length); i++){
			console.log("a");
			var pID = options.markers[i].event.id + "p";
			string += "<p id=" + pID + ">" + options.markers[i].event.eventName + "</p>";
			$("#map").on("click", "#" + pID, {event: options.markers[i]}, function(e){
				options.marker.target.closePopup();
				clickMarker(options.marker, e.data.event);
			}).on("hover", "#" + pID, function(e){
				$(this).css("background-Color", "#F4FA58");
			}).on("mouseout", "#" + pID, function(e){
				$(this).css("background-Color", "#FFFFFF");
			});
		}
		
	
		if(options.markers.length > 15){
			string += "<p> +" + (options.markers.length - 15) + " other events</p>";
		}
	
	
		marker.target.bindPopup(string).openPopup();
	}
}

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

function addEntryToInfobox(tag, margin, desc, value, hover){
	var divID = tag + "Div";
	if(margin){
		$("#info").append($("<div id=" + divID + " class=infoDivMarginLeft>"));
	}
	else{		
		$("#info").append($("<div id=" + divID + " class=infoDiv>"));
	}
	if(desc != null){	
		$("#" + tag + "Div").append($("<p id=" + tag + " class=infoDesc>").text(desc + ":"));
	}
	if(value != null){		
		$("#" + tag + "Div").append($("<p id=" + tag + "Value class=infoValue>").text(value));
	}
	if(hover){
		$("#" +divID).mouseover(function(){
			$(this).css("cursor", "pointer");
			$("#" + tag + "Value").css("background-Color", "#F4FA58");
		});
		$("#" + divID).mouseout(function(){
			$("#" + tag + "Value").css("background-Color", "#FFFFFF");
		});
	}
}

function infoboxEvent(){
	$("#info").empty();
	$("#image").remove();
	addEntryToInfobox("eventName", false, "Name", currentEvent.event.eventName);
	addEntryToInfobox("eventDescription", false, "Description", (currentEvent.event.description == "" ? "Not specified" : currentEvent.event.description.replace(/<(?:.|\n)*?>/gm, '')));
	addEntryToInfobox("eventCity", false, "City", currentEvent.location.cityName, (currentEvent.location.dbpedia_res_city != null ? true : false));
	addEntryToInfobox("eventRegion", false, "Region", currentEvent.location.region, (currentEvent.location.dbpedia_res_region != null ? true : false));
	addEntryToInfobox("eventCountry", false, "Country", currentEvent.location.country, (currentEvent.location.dbpedia_res_country != null ? true : false));
	addEntryToInfobox("eventLocation", false, "Location", currentEvent.location.locationName);
	var bands = currentEvent.bands;
	if(bands.length != 0){
		addEntryToInfobox("performer", false, "Performer", null);
		for(var i = 0; i < bands.length; i++){
			var idName = "band" + bands[i].name.replace(/[^a-zA-Z0-9]/gmi, "").replace(/\s+/g, "");;
			addEntryToInfobox(idName, true, null, bands[i].name, (bands[i].dbpedia_resource != null ? true : false));
			if(bands[i].dbpedia_resource != null){				
				$("#" + idName + "Div").click({band: bands[i]}, function(data){
					makeBandRequests(data.data.band);
					if(data.data.band.members.length == 1 && data.data.band.members[0].member_type == "A"){						
						$("#navigation").append($("<p class=navElement id=navBand>").text("Performer >"));
					}else{
						$("#navigation").append($("<p class=navElement id=navBand>").text("Band >"));
					}
					$("#navBand").click(function(){
						jQuery.each($(this).nextAll(), function(){
							$(this).remove();
							infoboxBand();
						});
					});
				});
			}
			
		}			
	}else{
		addEntryToInfobox("noPerformer", false, null, "No Performer specified.");
	}
	addEntryToInfobox("eventDate", false, "Date", currentEvent.event.start_date);
	addEntryToInfobox("eventTime", false, "Start time", currentEvent.event.start_time);
	if(currentEvent.event.end_date != "null"){
		addEntryToInfobox("eventEndDate", false, "End date", currentEvent.event.end_date);
	}
	addEntryToInfobox("eventEnd", false, "End time", (currentEvent.event.end_time == "null" ? "Not specified" : currentEvent.event.end_time) );
	
	if(currentEvent.location.dbpedia_res_city != null){
		$("#eventCityDiv").click(function(){
			if(currentCity == null){					
				makeCityRequests();
			}else{
				infoboxCity();	
			}
			$("#navigation").append($("<p class=navElement id=navCity>").text("City >"));
			$("#navCity").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
					infoboxCity();
				});
			});
		});
	}
	
	if(currentEvent.location.dbpedia_res_region != null){
		$("#eventRegionDiv").click(function(){
			if(currentRegion == null){					
				makeRegionRequests();
			}else{
				infoboxRegion();	
			}
			$("#navigation").append($("<p class=navElement id=navRegion>").text("Region >"));
			$("#navRegion").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
					infoboxRegion();
				});
			});
		});
	}
	
	if(currentEvent.location.dbpedia_res_country != null){
		$("#eventCountryDiv").click(function(){
			if(currentCountry == null){
				makeCountryRequests();
			}else{
				infoboxCountry();
			}
			$("#navigation").append($("<p class=navElement id=navCountry>").text("Country >"));
			$("#navCountry").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
					infoboxCountry();
				});
			});
		});
	}	
}

function infoboxCity(){
	$("#info").empty();
	addEntryToInfobox("cityName", false, "Name", currentEvent.location.cityName);
	if(currentCity.description != undefined){
		addEntryToInfobox("cityDescription", false, "Description", currentCity.description);
	}
	addEntryToInfobox("cityRegion", false, "Region", currentEvent.location.region, (currentEvent.location.dbpedia_res_region != null ? true : false));
	addEntryToInfobox("cityCountry", false, "Country", currentEvent.location.country, (currentEvent.location.dbpedia_res_country != null ? true : false));
	if(currentCity.population != undefined){
		addEntryToInfobox("cityPopulation", false, "Population", currentCity.population);
	}
	if(currentCity.area != undefined){
		addEntryToInfobox("cityArea", false, "Area", currentCity.area);
	}
	
	if(currentEvent.location.dbpedia_res_region != null){
		$("#cityRegionDiv").click(function(){
			if(currentRegion == null){				
				makeRegionRequests();
			}
			else{
				infoboxRegion();
			}
			$("#navigation").append($("<p class=navElement id=navRegion>").text("Region >"));
			$("#navRegion").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
				});
				infoboxRegion();
			});
		});
	}
	
	if(currentEvent.location.dbpedia_res_country != null){
		$("#cityCountryDiv").click(function(){
			if(currentCountry == null){				
				makeCountryRequests();
			}
			else{
				infoboxCountry();
			}
			$("#navigation").append($("<p class=navElement id=navCountry>").text("Country >"));
			$("#navCountry").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
				});
				infoboxCountry();
			});
		});
	}

	$("#image").remove();
	if(currentCity.image != undefined){
		addImage(currentCity.image);
	}
}

function infoboxRegion(){
	$("#info").empty();
	addEntryToInfobox("regionName", false, "Name", currentEvent.location.region);
	if(currentRegion.description != undefined){
		addEntryToInfobox("regionDescription", false, "Description", currentRegion.description);
	}
	addEntryToInfobox("regionCountry", false, "Country", currentEvent.location.country, (currentEvent.location.dbpedia_res_country != null ? true : false));
	if(currentRegion.population != undefined){
		addEntryToInfobox("regionPopulation", false, "Population", currentRegion.population);
	}
	if(currentRegion.area != undefined){		
		addEntryToInfobox("regionArea", false, "Area", currentRegion.area);
	}
	
	if(currentEvent.location.dbpedia_res_country != ""){
		$("#regionCountryDiv").click(function(){
			if(currentCountry == null){				
				makeCountryRequests();
			}
			else{
				infoboxCountry();
			}
			$("#navigation").append($("<p class=navElement id=navCountry>").text("Country >"));
			$("#navCountry").click(function(){
				jQuery.each($(this).nextAll(), function(){
					$(this).remove();
				});
				infoboxCountry();
			});
		});
	}

	$("#image").remove();
	if(currentRegion.image != undefined){
		addImage(currentRegion.image);
	}
}

function infoboxCountry(){
	$("#info").empty();
	addEntryToInfobox("countryName", false, "Name", currentEvent.location.country);
	if(currentCountry.description != undefined){
		addEntryToInfobox("countryDescription", false, "Description", currentCountry.description);
	}
	if(currentCountry.capital != undefined){		
		addEntryToInfobox("countryCapital", false, "Capital", currentCountry.capital);
	}
	if(currentCountry.populationDensity != undefined){		
		addEntryToInfobox("countryPopulationDensity", false, "Population Density", currentCountry.populationDensity);
	}
	if(currentCountry.area!= undefined){		
		addEntryToInfobox("countryArea", false, "Area", currentCountry.area);
	}

	$("#image").remove();
	if(currentCountry.image != undefined){
		addImage(currentCountry.image);
	}
}

function infoboxBand(){
	$("#info").empty();
	addEntryToInfobox("bandName",false, "Name", currentBand.name);
	if(currentBand.description != undefined){
		addEntryToInfobox("bandDescription", false, "Description", currentBand.description);
	}
	var members = currentBand.members;
	if(members.length != 0 && members[0].member_type != "A"){
		$("#info").append($("<p>").text("Member:"));
		for(var i = 0; i < members.length; i++){
			var idName = "band" + members[i].name.replace(/[^a-z0-9]/gmi, "").replace(/\s+/g, "");;
			addEntryToInfobox(idName, true, null, members[i].name, (members[i].dbpedia_resource != null ? true : false));
			if(members[i].dbpedia_resource != null){				
				$("#" + idName + "Div").click({member : members[i]}, function(data){
					makeMemberRequests(data.data.member);
					$("#navigation").append($("<p class=navElement id=navMember>").text("Member >"));
					$("#navMember").click(function(){
						jQuery.each($(this).nextAll(), function(){
							$(this).remove();
							infoboxMember();
						});
					});
				})
			}
			
		}			
	}

	$("#image").remove();
	if(currentBand.image != undefined){
		addImage(currentBand.image);
	}
}

function infoboxMember(){
	$("#info").empty();
	addEntryToInfobox("memberName", false, "Name", currentMember.name);
	if(currentMember.description != undefined){
		addEntryToInfobox("memberDescription", false, "Description", currentMember.description);
	}

	$("#image").remove();
	if(currentMember.image != undefined){
		addImage(currentMember.image);
	}
}

$(document).ready(function(){
	$("#navEvent").click(function(){
		jQuery.each($(this).nextAll(), function(){
			$(this).remove();
		});
		infoboxEvent();
	});
			
	map = L.map('map', {
		attributionControl : false,
		center : [ 20.0, 5.0 ],
		zoomControl : false,
		minZoom : 2,
		zoom : 2
	});
	
	L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png',{
		subdomains : [ 'otile1', 'otile2', 'otile3', 'otile4' ]
	}).addTo(map);
	
	map.on("zoomend", function(e){
		if($(this)[0]._zoom < 9 && $("#floatingInfo").css("visibility") == "visible"){
			$("#floatingInfo").animate({"width": "-=20%" }, 200, function(){
				$(this).css("visibility", "hidden");
			});
		}
		else if($(this)[0]._zoom >= 9 && $("#infobox").css("visibility") == "hidden" && !showInfo){
			if($("#floatingInfo").css("visibility") == "hidden"){
				$("#floatingInfo").css("visibility", "visible");
				$("#floatingInfo").animate({"width": "+=20%"}, 200);
			}
			updateFloatingInfobox();
		}
		else if($("#infobox").css("visibility") == "visible" && $(this)[0]._zoom < 15){
			showInfo = false;
			$("#infobox").css("visibility", "hidden");
			$("#social").css("visibility", "hidden");
			$("#socialInfo").css("visibility", "hidden");
			$("#socialResult").css("visibility", "hidden");
			$("#socialLoading").css("visibility", "hidden");
			$("#socialChart").empty();
			$("#socialChart").css("visibility", "hidden");
			$("#floatingInfo").css("visibility", "visible");
			$("#floatingInfo").animate({"width": "+=20%"}, 200);
			updateFloatingInfobox();
			if(hoverCountry != null){
				countryOver(hoverCountry);
			}
		}
	});
	
	map.on("dragend", function(e){
		if($("#floatingInfo").css("visibility") == "visible"){
			updateFloatingInfobox();
		}
	});
	var opts = {
			lines: 13, // The number of lines to draw
			length: 20, // The length of each line
			width: 10, // The line thickness
			radius: 20, // The radius of the inner circle
			scale: 1, // Scales overall size of the spinner
			corners: 1, // Corner roundness (0..1)
			rotate: 0, // The rotation offset
			direction: 1, // 1: clockwise, -1: counterclockwise
			color: '#000', // #rgb or #rrggbb or array of colors
			speed: 1, // Rounds per second
			trail: 60, // Afterglow percentage
			shadow: false, // Whether to render a shadow
			hwaccel: false, // Whether to use hardware acceleration
			className: 'spinner', // The CSS class to assign to the spinner
			zIndex: 2e9, // The z-index (defaults to 2000000000)
			top: '50%', // Top position relative to parent
			left: '50%' // Left position relative to parent
	};
	var loadingDiv = document.getElementById("containerLoading");
	var spinner = new Spinner(opts).spin(loadingDiv);
	//addCountries();
	$.ajax({ // ajax call starts
	    url: 'events', // JQuery loads serverside.php
	    dataType: 'json', // Choosing a JSON datatype
	}).done(function(data) { // Variable data contains the data we get from serverside
		var markers = L.markerClusterGroup();
		tree = markers.addTree(data);
		$.when(map.addLayer(markers)).then(function(){
			$(loadingDiv).css("visibility", "hidden");
		});
	});
	
	/* Useful to draw borders
	var borders = [];
	map.on("click", function(e) {
		borders[borders.length] = [e.latlng.lng, e.latlng.lat];
		var string = "[[";
		for(var i = 0; i<borders.length; i++){
			string += "[" + borders[i][0] + ", " + borders[i][1] + "],";
		}
		string += "]]";
		console.log(string);
	});
	*/
	
	L.Mask = L.Polygon.extend({
		options: {
			stroke: false,
			color: '#333',
			fillOpacity: 0.5,
			clickable: true,
	
			outerBounds: new L.LatLngBounds([-90, -360], [90, 360])
		},
	
		initialize: function (latLngs, options) {
	        
	         var outerBoundsLatLngs = [
				this.options.outerBounds.getSouthWest(),
				this.options.outerBounds.getNorthWest(),
				this.options.outerBounds.getNorthEast(),
				this.options.outerBounds.getSouthEast()
			];
	        var bounds = [];
	        bounds[0] = outerBoundsLatLngs;
	        jQuery.each(latLngs, function(){
	        	bounds.push($(this));
	        });
	        L.Polygon.prototype.initialize.call(this, bounds, options);	
		},
	
	});
});