var map;
var showInfo = false;
var hoverCountry = null;
var onGreyLayer = false;

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

function pullOutInfobox2(){
	if($("#pullButton").css("visibility") === "hidden"){
		$("#pullButton").css("visibility", "visible");
	}
	$("#pullButton").animate({"top": "46%"}, 200, function(){
		$(this).css("transform", "rotate(180deg)");
		$(this).animate({"left": "53%"}, 200);
		$("#infobox2").css("visibility", "visible");
		$("#infobox2").animate({"width": "46%"}, 200);
	});
};

function pullBackInfobox2(){
	$("#infobox2").animate({"width": "0"}, 200, function(){
		$(this).css("visibility", "hidden");
	});
	$("#pullButton").animate({"left": "99%"}, 200, function(){
		$(this).css("transform", "rotate(0deg)");
		$(this).animate({"top": "0%"}, 200);
	});
};

function isMarkerVisible(marker){
	var coordinates = [ 
	                   [map.getBounds()._southWest.lat, map.getBounds()._southWest.lng], 
	                   [map.getBounds()._southWest.lat, map.getBounds()._northEast.lng], 
	                   [map.getBounds()._northEast.lat, map.getBounds()._northEast.lng], 
	                   [map.getBounds()._northEast.lat, map.getBounds()._southWest.lng]
	                   ];
	return isInCoordinates(coordinates, marker._latlng.lng, marker._latlng.lat);
}

function updateFloatingInfobox(){
	$("#floatingList").empty();				
	for(var i = 0; i<currentMarkers.length; i++){
		if(isMarkerVisible(currentMarkers[i])){
			$("#floatingList").append($("<li id="+currentMarkers[i]._leaflet_id+">").text(currentMarkers[i].options.name));
			$("#" + currentMarkers[i]._leaflet_id).click(function(e){
				for(var j = 0; j < currentMarkers.length; j++){
					if(currentMarkers[j]._leaflet_id == e.currentTarget.id){
						clickMarker(currentMarkers[j]);
					}
				}
			}).hover(function(e){
				$(this).css("background-Color", "#F4FA58");
				for(var j = 0; j < currentMarkers.length; j++){
					if(currentMarkers[j]._leaflet_id == e.currentTarget.id){
						hoverMarker(currentMarkers[j]);
					}
				}
			}).mouseout(function(e){
				$(this).css("background-Color", "#FFFFFF");
				for(var j = 0; j < currentMarkers.length; j++){
					if(currentMarkers[j]._leaflet_id == e.currentTarget.id){
						hoverExitMarker(currentMarkers[j]);
					}
				}
			});
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

function zoom() {
	//map.setView(new L.LatLng(e.latlng.lat, e.latlng.lng), 8);
	for (var i = 0; i < events.length; i++) {
		if (events[i].multiple != undefined) {
			events[currentMarkers.length] = L.marker(closerMarkers[i].position, {
				icon : redIcon,
				multiple : closerMarkers[i].multiple
			}).on("click",function(e) {
				//zoom closer to that point
				map.setView(e.latlng, 10);

				//remove the previous markers
				for ( var m = 0; m < currentMarkers.length; m++) {
					map.removeLayer(currentMarkers[m]);
				}

				//add center marker
				var centerMarker = L.marker(e.latlng, {
					icon : smallRedIcon
				}).addTo(map);

				//add a circle of markers around the center marker
				for ( var m = 0; m < e.target.options.multiple.length; m++) {
					var x = 0.025* Math.cos(m/ e.target.options.multiple.length* 2* Math.PI)+ e.latlng.lat;
					var y = 0.05* Math.sin(m/ e.target.options.multiple.length* 2* Math.PI)+ e.latlng.lng;
					L.marker([ x, y ],
					{
						icon : redIcon,
						text : e.target.options.multiple[m].text
					}).on("click", function(e) {
						infobox.style.visibility = "visible";
						infobox.innerHTML = "<p>Name: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>Description: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>City: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>Country: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>Location: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>Start time: " + e.target.options.text + "</p>";
						infobox.innerHTML += "<p>Duration: " + e.target.options.text + "</p>";
					}).addTo(map);

					var linePoints = [ [ x, y ], e.latlng ];
					L.polyline(linePoints, {
						color : "red",
						weight : 2,
						smoothFactor : 1
					}).addTo(map);
				}
			}).addTo(map);
		} else {
			currentMarkers[currentMarkers.length] = L.marker(events[i].latlng, {
				icon : redIcon,
				name : events[i].name,
				description : events[i].description,
				city : events[i].city,
				country : events[i].country
			}).on("click", function(e) {
				clickMarker(e);
			}).on("mouseover", function(e){
				hoverMarker(e);
			}).on("mouseout", function(e){
				hoverExitMarker(e);
			}).addTo(map);
		}
	}
}

function hoverMarker(marker){
	if(marker.target != undefined){				
		marker.target.setIcon(redIconHover);
	}else{
		marker.setIcon(redIconHover);
	}
}

function hoverExitMarker(marker){
	if(marker.target != undefined){
		marker.target.setIcon(redIcon);
	}else{
		marker.setIcon(redIcon);
	}
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

function clickMarker(marker){
	showInfo = true;
	removeCountries();
	var tmpLatLng = marker.latlng == undefined ?  $.extend( {}, marker._latlng  ): $.extend({}, marker.latlng  );
	tmpLatLng.lng += 0.01021508561708;
	tmpLatLng.lat -= 0.003382020592895;
	var options = marker.target == undefined ? marker.options : marker.target.options;
	map.setView(tmpLatLng, 16);
	$("#infobox").css("visibility", "visible");
	if($("#floatingInfo").css("visibility") === "visible"){				
		$("#floatingInfo").animate({"width": "-=20%" }, 200, function(){
			$(this).css("visibility", "hidden");
		});
	}
	
	$("#social").css("visibility", "visible");
	$("#socialInfo").css("visibility", "visible");
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
	console.log(options);
	$("#socialInfoP").html("Sentiment data is processed");
	$.getJSON( "/getEventById", { id : options.ids[0] }).done(function(result){
		console.log(result);
		currentEvent = result;
		currentCity = null;
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
		console.log(JSON.stringify(terms));
		$.getJSON( "/getSentiment", { terms: JSON.stringify(terms), location: replaceCharacters(currentEvent.location.cityName) } ).done(function(result){
			$("#socialInfoP").html("Sentiment data");
			$("#socialLoading").css("visibility", "hidden");					
			$("#socialChart").css("visibility", "visible");			
			$("#socialResult").css("visibility", "visible");
			$("#socialResult").empty();

			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Passion:")));
			$("#socialResult").append($("<div class=socialEntryBig>").append($("<p id=socialPassion>").text(result.score_passion)));

			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Reach:")));
			$("#socialResult").append($("<div class=socialEntryBig>").append($("<p id=socialReach>").text(result.score_reach)));

			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Sentiment:")));
			$("#socialResult").append($("<div class=socialEntryBig>").append($("<p id=socialSentiment>").text(result.score_sentiment)));

			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Strength:")));
			$("#socialResult").append($("<div class=socialEntryBig>").append($("<p id=socialStrength>").text(result.score_strength)));

			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Keywords:")));
			if(result.keywords.length != 0){
				$("#socialResult").append($("<div class=socialEntryBig>").append($("<p>").text(result.keywords[0].keyword + "\t" + result.keywords[0].occurance)));
				for(var i = 1; i < result.keywords.length; i++){
					$("#socialResult").append($("<div class=socialDesc>"));
					$("#socialResult").append($("<div class=socialEntry>").append($("<p>").text(result.keywords[i].keyword + "\t" + result.keywords[i].occurance)));
				}
			}else{
				$("#socialResult").append($("<div class=socialEntryBig>").append($("<p>").text("No keywords found.")));
			}
			
			$("#socialResult").append($("<div class=socialDescBig>").append($("<p>").text("Hashtags:")));
			if(result.hashtags.length != 0){
				$("#socialResult").append($("<div class=socialEntryBig>").append($("<p>").text(result.hashtags[0].hashtag + "\t" + result.hashtags[0].occurance)));
				for(var i = 1; i < result.hashtags.length; i++){
					$("#socialResult").append($("<div class=socialDesc>"));
					$("#socialResult").append($("<div class=socialEntry>").append($("<p>").text(result.hashtags[i].hashtag + "\t" + result.hashtags[i].occurance)));
				}
			}else{
				$("#socialResult").append($("<div class=socialEntryBig>").append($("<p>").text("No hashtags found.")));
			}
			
			var totalSentiment = result.sentiment.positive + result.sentiment.negative;
			
			
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

		});
	})
}

function makeCityRequests(){
	var uri = currentEvent.location.dbpedia_resource;
	currentCity = {};
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		currentCity.description = result.results.bindings[0].res.value;
		infoboxCity();
	});
					
	sparqlRequest(uri, "http://dbpedia.org/ontology/populationTotal", false).done(function(result) {
		currentCity.population = result.results.bindings[0].res.value;
		infoboxCity();
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/areaTotal", false).done(function(result) {
		currentCity.area = result.results.bindings[0].res.value;
		infoboxCity();
	});
}

function makeCountryRequests(){
	var uri = "http://dbpedia.org/resource/" + currentEvent.location.country.split(' ').join('_');
	currentCountry = {};
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		currentCountry.description = result.results.bindings[0].res.value;
		infoboxCountry();
	});
	
	sparqlRequestWithHTML(uri, "http://dbpedia.org/ontology/capital").done(function(result) {
		sparqlRequest(result.results.bindings[0].res.value, "http://dbpedia.org/property/name", true).done(function(result){
			currentCountry.capital = result.results.bindings[0].res.value;
			infoboxCountry();
		});
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/PopulatedPlace/areaTotal", false).done(function(result) {
		console.log(result);
		currentCountry.populationDensity = result.results.bindings[0].res.value;
		infoboxCountry();
	});
	
	sparqlRequest(uri, "http://dbpedia.org/ontology/areaTotal", false).done(function(result) {
		currentCountry.area = result.results.bindings[0].res.value;
		infoboxCountry();
	});
}

function makeBandRequests(band){
	var uri = band.dbpedia_resource;
	currentBand = { "name" : band.name,
					"members" : band.members };
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		currentBand.description = result.results.bindings[0].res.value;
		infoboxBand();
	});
}
function makeMemberRequests(member){
	var uri = member.dbpedia_resource;
	currentMember = { "name" : member.name };
	sparqlRequest(uri, "http://dbpedia.org/ontology/abstract", true).done(function(result) {
		currentMember.description = result.results.bindings[0].res.value;
		infoboxMember();
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

function infoboxEvent(){
	$("#info").empty();
	$("#info").append($("<p id=eventName>").text("Name: " + currentEvent.event.eventName));
	$("#info").append($("<p id=eventDescription>").text("Description: " + (currentEvent.event.description == "" ? "Not specified" : currentEvent.event.description.replace(/<(?:.|\n)*?>/gm, ''))));
	$("#info").append($("<p id=eventCity>").text("City: " + currentEvent.location.cityName));
	$("#info").append($("<p id=eventRegion>").text("Region: " + currentEvent.location.region));
	$("#info").append($("<p id=eventCountry>").text("Country: " + currentEvent.location.country));
	$("#info").append($("<p id=eventLocation>").text("Location: " + currentEvent.location.locationName));
	var bands = currentEvent.bands;
	if(bands.length != 0){
		$("#info").append($("<p>").text("Performer:"));
		for(var i = 0; i < bands.length; i++){
			var idName = "band" + bands[i].name.replace(/[^a-zA-Z0-9]/gmi, "").replace(/\s+/g, "");;
			$("#info").append($("<p id=" + idName + " class=infoMarginLeft>").text(bands[i].name));
			$("#" + idName).mouseover(function(){
				$(this).css("background-Color", "#F4FA58");
			});
			$("#" + idName).mouseout(function(){
				$(this).css("background-Color", "#FFFFFF");
			});
			
			$("#" + idName).click({band: bands[i]}, function(data){
				makeBandRequests(data.data.band);
				$("#navigation").append($("<p class=navElement id=navBand>").text("Band >"));
				$("#navBand").click(function(){
					jQuery.each($(this).nextAll(), function(){
						$(this).remove();
						infoboxBand();
					});
				});
			})
			
		}			
	}else{
		$("#info").append($("<p>").text("No Performer specified."));
	}
	
	$("#info").append($("<p id=eventType>").text("Type: " + (currentEvent.event.event_type == null ? "Not specified" : currentEvent.event.event_type)));
	$("#info").append($("<p id=eventTime>").text("Start time: " + currentEvent.event.date));
	$("#info").append($("<p id=eventDuration>").text("Duration: " + currentEvent.duration));
	
	$("#eventCity").click(function(){
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
	})
	$("#eventCity").mouseover(function(){
		$(this).css("background-Color", "#F4FA58");
	});
	$("#eventCity").mouseout(function(){
		$(this).css("background-Color", "#FFFFFF");
	});
	
	$("#eventCountry").click(function(){
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
	})
	$("#eventCountry").mouseover(function(){
		$(this).css("background-Color", "#F4FA58");
	});
	$("#eventCountry").mouseout(function(){
		$(this).css("background-Color", "#FFFFFF");
	});
	
	$("#eventLocation").click(function(){
		//to-do
	})
	$("#eventLocation").mouseover(function(){
		$(this).css("background-Color", "#F4FA58");
	});
	$("#eventLocation").mouseout(function(){
		$(this).css("background-Color", "#FFFFFF");
	});
	
}

function infoboxCity(){
	$("#info").empty();
	$("#info").append($("<p id=cityName>").text("Name: " + currentEvent.location.cityName));
	$("#info").append($("<p id=cityDescription>").text("Description: " + currentCity.description));
	$("#info").append($("<p id=cityRegion>").text("Region: " + currentEvent.location.region));
	$("#info").append($("<p id=cityCountry>").text("Country: " + currentEvent.location.country));
	$("#info").append($("<p id=cityPopulation>").text("Population: " + currentCity.population));
	$("#info").append($("<p id=cityArea>").text("Area: " + currentCity.area));
	
	$("#cityCountry").click(function(){
		makeCountryRequests(currentCity.country);
		$("#navigation").append($("<p class=navElement id=navCountry>").text("Country >"));
		$("#navCountry").click(function(){
			jQuery.each($(this).nextAll(), function(){
				$(this).remove();
			});
			infoboxCountry();
		});
	})
	$("#cityCountry").mouseover(function(){
		$(this).css("background-Color", "#F4FA58");
	});
	$("#cityCountry").mouseout(function(){
		$(this).css("background-Color", "#FFFFFF");
	});
}

function infoboxCountry(){
	$("#info").empty();
	$("#info").append($("<p id=countryName>").text("Name: " + currentEvent.location.country));
	$("#info").append($("<p id=countryDescription>").text("Description: " + currentCountry.description));
	$("#info").append($("<p id=countryCapital>").text("Capital: " + currentCountry.capital));
	$("#info").append($("<p id=countryPopulationDensity>").text("Population Density: " + currentCountry.populationDensity));
	$("#info").append($("<p id=countryArea>").text("Area: " + currentCountry.area));
}

function infoboxBand(){
	$("#info").empty();
	$("#info").append($("<p id=bandName>").text("Name: " + currentBand.name));
	$("#info").append($("<p id=bandDescription>").text("Description: " + currentBand.description));
	var members = currentBand.members;
	if(members.length != 0){
		$("#info").append($("<p>").text("Member:"));
		for(var i = 0; i < members.length; i++){
			var idName = "band" + members[i].name.replace(/[^a-z0-9]/gmi, "").replace(/\s+/g, "");;
			$("#info").append($("<p id=" + idName + " class=infoMarginLeft>").text(members[i].name));
			$("#" + idName).mouseover(function(){
				$(this).css("background-Color", "#F4FA58");
			});
			$("#" + idName).mouseout(function(){
				$(this).css("background-Color", "#FFFFFF");
			});
			
			$("#" + idName).click({member : members[i]}, function(data){
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
	}else{
		$("#info").append($("<p>").text("No Performer specified."));
	}
}

function infoboxMember(){
	$("#info").empty();
	$("#info").append($("<p id=memberName>").text("Name: " + currentMember.name));
	$("#info").append($("<p id=memberDescription>").text("Description: " + currentMember.description));
}

$(document).ready(function(){
	$("#navEvent").click(function(){
		jQuery.each($(this).nextAll(), function(){
			$(this).remove();
		});
		infoboxEvent();
	});
	
	$("#pullButton").click(function(){
		if($("#infobox2").css("visibility") === "visible"){				
			pullBackInfobox2();
		}else{
			pullOutInfobox2();
		}
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
		if($(this)[0]._zoom < 8 && $("#floatingInfo").css("visibility") == "visible"){
			$("#floatingInfo").animate({"width": "-=20%" }, 200, function(){
				$(this).css("visibility", "hidden");
			});
		}
		else if($(this)[0]._zoom >= 8 && $("#infobox").css("visibility") == "hidden" && !showInfo){
			if($("#floatingInfo").css("visibility") == "hidden"){
				$("#floatingInfo").css("visibility", "visible");
				$("#floatingInfo").animate({"width": "+=20%"}, 200);
			}
			updateFloatingInfobox();
		}
		else if($("#infobox").css("visibility") == "visible" && $(this)[0]._zoom < 15){
			showInfo = false;
			$("#infobox").css("visibility", "hidden");
			$.when($.ajax(pullBackInfobox2())).then(function () {
				$("#pullButton").css("visibility", "hidden");
			});
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
	
	map.on("moveend", function(e){
		if($("#floatingInfo").css("visibility") == "visible"){
			updateFloatingInfobox();
		}
	});
	
	//addCountries();
	$.ajax({ // ajax call starts
	    url: 'events', // JQuery loads serverside.php
	    dataType: 'json', // Choosing a JSON datatype
	}).done(function(data) { // Variable data contains the data we get from serverside
		console.log(data);
		var tree = data;
		//var tree = tmp;
		// new 
		var markers = L.markerClusterGroup();
		markers.addTree(tree);
		//var map = L.map('map', { center: latlng, zoom: 2, layers: [tiles] });
		map.addLayer(markers);
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