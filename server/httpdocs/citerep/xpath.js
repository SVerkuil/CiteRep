/*
 * jQuery plugin: fieldSelection - v0.1.1 - last change: 2006-12-16
 * (c) 2006 Alex Brem <alex@0xab.cd> - http://blog.0xab.cd
 */
(function(){var fieldSelection={getSelection:function(){var e=(this.jquery)?this[0]:this;return(('selectionStart'in e&&function(){var l=e.selectionEnd-e.selectionStart;return{start:e.selectionStart,end:e.selectionEnd,length:l,text:e.value.substr(e.selectionStart,l)}})||(document.selection&&function(){e.focus();var r=document.selection.createRange();if(r===null){return{start:0,end:e.value.length,length:0}}var re=e.createTextRange();var rc=re.duplicate();re.moveToBookmark(r.getBookmark());rc.setEndPoint('EndToStart',re);return{start:rc.text.length,end:rc.text.length+r.text.length,length:r.text.length,text:r.text}})||function(){return null})()},replaceSelection:function(){var e=(typeof this.id=='function')?this.get(0):this;var text=arguments[0]||'';return(('selectionStart'in e&&function(){e.value=e.value.substr(0,e.selectionStart)+text+e.value.substr(e.selectionEnd,e.value.length);return this})||(document.selection&&function(){e.focus();document.selection.createRange().text=text;return this})||function(){e.value+=text;return jQuery(e)})()}};jQuery.each(fieldSelection,function(i){jQuery.fn[i]=this})})();

/**
* Automatically obtain xpath from click in textarea
* Permission for use given to CitRep Project
* @Copyright IOweb - ioweb.nl
*/

//Current xpath input field that we are editing
var current = null;

//Old values
var old_url = '';
var old_shortcode = '';
var hidden = false;

$(document).ready(function() {
	
	//Click in textarea
	$('#in').click(function() {
	
		//Check if textarea is selected
		if(current!=null) {
		
			//.start, .end, .length, .text
			var range = $(this).getSelection();
			
			//Obtain html if any
			if(range.start>1) {
				var html = 	$(this).val()
							.substr(0,range.start)
							.replace(/\&lt\;/g,'<') //decode tags
							.replace(/\&gt\;/g,'>') //decode tags
							.replace(/(\r|\n|\t|\s\s+)/gm,'') //Remove white spaces
							.replace(/\<\?.+\?\>/g,'') //Remove <? xml version ?> tags
							.replace(/\<staffel\>.+\<\/staffel\>/g,'') //Remove <staffel> tags
							.replace(/\<\!\-\-.+\-\-\>/g,'') //Remove comments
							.replace(/\<[^\<\>]+\s?\/\s?>/g,'') //Remove self closing tags
							.replace(/\<\!\[CDATA[^\<\>]+\]\]\>/g,"") //Remove CDATA
							.replace(/\<\!\[CDATA.+\]\]\>/g,"") //Remove CDATA
							.replace(/\sid\=\"[0-9a-zA-z\.\_]+\"/gi,""); //Remove id="xxxx"
				var theXpath = GetXpath(html);
				$(current).val(theXpath);
			}
		
		}
	});
	
	//Input veld afvangen
	$('input[type="text"]',$('#xpath_div')).click(function() {
		current = this;
		$('input[type="text"]',$('#xpath_div')).each(function() {
			$(this).css('border-color','');
		});
		$(this).css('border-color','blue');
	});
});

/**
* Obtain xpath from string TILL wanted text
*/
function GetXpath(str) {
	var xpath = '';
	var xpath_array = {};
	var match = str.match(/\<([^\<\>]+)\>/g);

	//Check if string contains any tags at all
	if(match!=null) {
		var copy = match; //make copy
		for(num in match) { //loop tags
			var tag = match[num].match(/\<([A-Za-z0-9\_\:]+)/);
			if(tag!=null) { //check if found
				name = tag[1]; //get name of tag e.g. "body" or "field"
				for(find=num;find<match.length;find++) { //Check if ending tag is found
					var re = new RegExp('^\<\/'+name.replace('_','\_')+'\>','g');
					if(match[find].replace(/\s/,'').match(re)) {
						copy[find] = ''; copy[num] = ''; //remove tags
						break; //Stop searching for end tag
					}
				}
			}
		}
		//alert(dump(copy));

		//Make actual xpath from array above
		for(num in copy) {
			var item = copy[num].match(/\<(.+)\>/);
			if(item!=null) {
				item = item[1].replace(' = ','=');
				var parts = item.split(' '); //Split on whitespace
				if(parts.length>1) { //Special properties (e.g. key="val")
					xpath += '/'+parts[0]+'[';
					var and = false;
					for(var i=1;i<parts.length;i++) {
						var tags = parts[i].match(/([A-Za-z0-9\_]+)\=(\"|\')(.+)(\"|\')/);
						/*if(tags!=null) {
							//Ignore general tags (nm fix)
							if(	tags[1]!='xmlns' &&
								tags[1]!='xsi') {
									xpath += (and?' and ':'')+'@'+tags[1]+'="'+tags[3]+'"';
									and = true;
							}
						}*/
					}
					xpath += ']';
				} else {
					xpath += '/'+parts[0];
				}	
			}		
		}
		
		//Check if we need to select a property instead of an tag
		//e.g. select <tag id="identify" property="data_here"> instead of <tag>data_here</tag>
		var lastpart = str.match(/\<([A-Za-z0-9\_\:]+) ([^\<\>]+)$/);
		if(lastpart!=null) {
			xpath += '/'+lastpart[1]+'[';
			var parts = lastpart[2].split(' ');
			var and = false;
			for(var i=0;i<parts.length;i++) {
				var tags = parts[i].match(/([A-Za-z0-9\_\:]+)\=(\"|\')([^\"\']*)/);
				if(tags!=null) {
					if(parts.length==i+1) { //last tag
						xpath += ']/@'+tags[1];
					} else { //Add property to selector
						//xpath += (and?' and ':'')+'@'+tags[1]+'="'+tags[3]+'"';
						//and = true;
					}	
				}				
			}
		}
		
		//Remove empty selectors
		xpath = xpath.replace(/\[\]/g,'');
		
		//Remove double //
		xpath = xpath.replace(/\/\//g,'/');
		
		//Return xpath
		return xpath;
	} 
	return ''; //malformed
}