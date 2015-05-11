//adds all countries to the map
function addCountries(){
	for(var i = 0; i < borderData.features.length; i++){				
		countries.push(L.geoJson(borderData.features[i], {
			onEachFeature: OnEachCountry,
			style : normalStyle
		}).addTo(map));
		countries[countries.length-1].options.name = borderData.features[i].properties.name;
	}
}

//adds a specific country to the map by its name
function addCountry(name){
	for(var i = 0; i < borderData.features.length; i++){
		if(borderData.features[i].properties.name == name){					
			countries.push(L.geoJson(borderData.features[i], {
				onEachFeature: OnEachCountry,
				style : normalStyle
			}).addTo(map));
			countries[countries.length-1].options.name = borderData.features[i].properties.name;
		}
	}
}

//removes all countries from the map
function removeCountries(){
	for(var i = 0; i < countries.length; i++){				
		map.removeLayer(countries);
	}
}

//removes a specific country from the map by its name
function removeCountry(name){
	for(var i = 0; i < countries.length; i++){
		if(countries[i].options.name === name){
			map.removeLayer(countries[i]);
			break;
		}
	}
}

//defines the operations that are performed when interacting with the countries
function OnEachCountry(feature, layer){
	layer.on({
		click: countryClick,
		mouseover: countryOver,
		mouseout: countryOut
	})
}

//describes the action, which is performed on click on a country
function countryClick(e){
	if(map._zoom < 15){
		onGreyLayer = false;
		removeCountries();
		var greyLayer = [];
		countryHover = null;
		/*var countryLayers = e.target.feature.geometry.coordinates;
		jQuery.each(countryLayers, function(){
			greyLayer[greyLayer.length] = [];
			jQuery.each($(this).length > 1 ? $(this) : $(this)[0], function(){
				greyLayer[greyLayer.length-1].push(new L.LatLng($(this)[1], $(this)[0]));
			});
		});
		var greyPolygon = L.mask(greyLayer).on("mouseover", function(e){
			if(onGreyLayer){
				map.removeLayer(e.target);
				addCountries();
			}
			onGreyLayer = true;
		}).addTo(map);
		greyPolygon.setStyle(greyStyle);*/
		map.fitBounds(e.target.getBounds());
		/*if(currentActiveCountries != null){
			currentActiveCountries.target.setStyle(normalStyle);
		}*/
		if(currentActiveCountries.length == 0){
			$("#filterbox").css("visibility", "visible");
		}
		var countryName = e.target.feature.properties.name;
		if(!isActive(countryName)){
			currentActiveCountries.push(countryName);
		}
		updateMarkers();
		$("#filterbox").append($("<p id="+countryName.split(" ").join("_").split(".").join("")+">").text(countryName));
		doBlink("#"+countryName.split(" ").join("_").split(".").join(""), 3);
		$("#"+countryName.split(" ").join("_").split(".").join("")).click(function(){
			$(this).slideUp();
			addCountry($(this)[0].innerText);
			if(currentActiveCountries.length == 1){
				currentActiveCountries.pop();
				$("#filterbox").css("visibility", "hidden");
			}
			for(var i = 0; i<currentActiveCountries.length; i++){
				if(currentActiveCountries[i] === $(this)[0].innerText){
					currentActiveCountries.splice(i,1);
				}
			}
			updateMarkers();
		});
		$("#"+countryName.split(" ").join("_").split(".").join("")).mouseover(function(){
			$(this).css("background-Color", "#F4FA58");
		});
		$("#"+countryName.split(" ").join("_").split(".").join("")).mouseout(function(){
			$(this).css("background-Color", "#FFFFFF");
		});
		removeCountry(countryName);
	}
}

//describes the action, which is performed on hover over a country
function countryOver(e){
	hoverCountry = e;
	if($("#infobox").css("visibility") === "hidden"){				
		e.target.setStyle(highlightStyle);
	}
}

//describes the action, which is performed on hover out of a country
function countryOut(e){
	e.target.setStyle(normalStyle);
}

//checks if a specific point is inside some coordinates
function isInCoordinates(coordinates, pointLat, pointLng){
	var inside = false;
	for (var i = 0, j = coordinates.length - 1; i < coordinates.length; j = i++) {
	 	var xi = coordinates[i][0], yi = coordinates[i][1];
	 	var xj = coordinates[j][0], yj = coordinates[j][1];
	 	var intersect = ((yi > pointLat) != (yj > pointLat)) && (pointLng < (xj - xi) * (pointLat - yi) / (yj - yi) + xi);
	 	if (intersect){
	 		inside = !inside;
	 	}
	 }
	return inside;
}

//checks if a country is active or not
function isActive(country){
	var active = false;
	jQuery.each(currentActiveCountries, function(){
		if($(this)[0] === country){
			active = true;
		}
	});
	return active;
}